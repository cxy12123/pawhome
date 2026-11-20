package com.pawhome.common.util;

import com.pawhome.common.constant.CommonConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 业务单号生成器。
 * <p>
 * 格式：前缀 + yyyyMMdd + 4 位当日流水，例如 PH202601120001。
 * 用 Redis INCR 保证多实例部署下也不会重号（数据库自增只能在单库内保证）。
 * Key 设置 2 天过期，避免长期堆积。
 * <p>
 * 单号可读性强，运营人员报单号就能知道是哪天、什么业务，
 * 比纯雪花ID更适合给业务方看。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoUtils {

    private final StringRedisTemplate stringRedisTemplate;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generate(String prefix) {
        String datePart = LocalDate.now().format(DATE_FMT);
        String redisKey = CommonConstants.REDIS_BIZ_NO + prefix + ":" + datePart;

        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1L) {
            // 第一次使用，设置 2 天过期
            stringRedisTemplate.expire(redisKey, Duration.ofDays(2));
        }
        return prefix + datePart + String.format("%04d", seq == null ? 0 : seq);
    }

    /**
     * 用数据库里当日的最大序号给 Redis 计数器兜底。
     * <p>
     * 为什么需要：计数器只存在 Redis 里，而本项目 Redis 没有开持久化，
     * 服务重启或 Redis 重启后计数器会从 1 重新开始，
     * 于是生成出已经存在的单号，被唯一索引挡下来。
     * <p>
     * 解决：服务启动时查一次"今天已有的最大号"，把计数器抬到不低于它。
     *
     * @param prefix      单号前缀，如 SR
     * @param dbMaxFinder 入参是当天的日期串，返回库里当天的最大序号（没有则 0）
     */
    public void seedIfLower(String prefix, java.util.function.Function<String, Long> dbMaxFinder) {
        String datePart = LocalDate.now().format(DATE_FMT);
        String redisKey = CommonConstants.REDIS_BIZ_NO + prefix + ":" + datePart;
        try {
            Long dbMax = dbMaxFinder.apply(datePart);
            if (dbMax == null || dbMax <= 0) {
                return;
            }
            String current = stringRedisTemplate.opsForValue().get(redisKey);
            long currentVal = current == null ? 0L : Long.parseLong(current);
            if (dbMax > currentVal) {
                stringRedisTemplate.opsForValue()
                        .set(redisKey, String.valueOf(dbMax), Duration.ofDays(2));
                log.info("单号计数器已对齐数据库：{} 当日最大 {}，Redis 原值 {}", prefix, dbMax, currentVal);
            }
        } catch (Exception e) {
            log.warn("单号计数器对齐失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 从形如 SR202601120003 的单号里取出末尾序号 */
    public static long tailSeq(String no) {
        if (no == null || no.length() < 4) {
            return 0L;
        }
        try {
            return Long.parseLong(no.substring(no.length() - 4));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public String animalNo() {
        return generate(CommonConstants.NO_ANIMAL);
    }

    public String adoptionNo() {
        return generate(CommonConstants.NO_ADOPTION);
    }

    public String donationNo() {
        return generate(CommonConstants.NO_DONATION);
    }

    public String supplyRecordNo() {
        return generate(CommonConstants.NO_SUPPLY_RECORD);
    }

    public String supplyItemNo() {
        return generate(CommonConstants.NO_SUPPLY_ITEM);
    }
}
