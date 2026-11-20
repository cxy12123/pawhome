package com.pawhome.donation.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.api.feign.SupplyClient;
import com.pawhome.common.core.R;
import com.pawhome.donation.entity.Donation;
import com.pawhome.donation.entity.DonationCompensation;
import com.pawhome.donation.mapper.DonationCompensationMapper;
import com.pawhome.donation.mapper.DonationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 捐赠入库补偿任务。
 * <p>
 * 每 30 秒扫描一次补偿表，把状态为 WAITING 且到了重试时间的记录重新投递。
 * <p>
 * 几个设计要点：
 * 1. 指数退避：失败次数越多，下次重试间隔越长，避免把下游打死；
 * 2. 最大重试 5 次：超过就标记为 FAILED 转人工处理，不无限重试；
 * 3. 单批处理 20 条：避免一次拉太多影响正常业务；
 * 4. 依赖 supply 侧接口幂等：即使重复投递也不会重复入库。
 * <p>
 * 关于"多个实例同时跑定时任务会重复执行"的问题：
 * 理论上需要分布式锁或 ShedLock 保证同一时刻只有一个实例执行。
 * 本项目因 supply 接口幂等，重复执行不会有数据问题；
 * 如果对下游有强要求，可以在方法上加 Redisson 锁解决。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonationCompensationTask {

    private final DonationCompensationMapper compensationMapper;
    private final DonationMapper donationMapper;
    private final SupplyClient supplyClient;

    private static final int MAX_RETRY = 5;
    private static final int BATCH_SIZE = 20;

    @Scheduled(initialDelay = 20000, fixedDelay = 30000)
    public void retryFailedInbound() {
        List<DonationCompensation> tasks = compensationMapper.selectList(
                new LambdaQueryWrapper<DonationCompensation>()
                        .eq(DonationCompensation::getStatus, "WAITING")
                        .le(DonationCompensation::getNextTime, LocalDateTime.now())
                        .lt(DonationCompensation::getRetryCount, MAX_RETRY)
                        .orderByAsc(DonationCompensation::getId)
                        .last("LIMIT " + BATCH_SIZE));

        if (tasks.isEmpty()) {
            return;
        }

        log.info("开始处理 {} 条捐赠补偿任务", tasks.size());

        for (DonationCompensation task : tasks) {
            try {
                Donation donation = donationMapper.selectById(task.getDonationId());
                if (donation == null) {
                    markFailed(task, "捐赠单已不存在");
                    continue;
                }

                R<Boolean> resp = supplyClient.inbound(SupplyInboundDTO.builder()
                        .requestId(donation.getDonationNo())
                        .itemId(donation.getSupplyItemId())
                        .itemName(donation.getItemDesc())
                        .quantity(donation.getQuantity())
                        .sourceType("DONATION")
                        .sourceId(donation.getId())
                        .operatorName("系统补偿")
                        .remark("补偿入库：" + donation.getDonationNo())
                        .build());

                if (resp.isSuccess()) {
                    // 补偿成功：单据转 STORED，任务转 SUCCESS
                    Donation done = new Donation();
                    done.setId(donation.getId());
                    done.setStatus("STORED");
                    done.setStoreTime(LocalDateTime.now());
                    donationMapper.updateById(done);

                    task.setStatus("SUCCESS");
                    task.setLastError(null);
                    compensationMapper.updateById(task);

                    log.info("补偿成功：捐赠单 {} 已完成入库", donation.getDonationNo());
                } else {
                    scheduleNextRetry(task, "补偿调用返回失败：" + resp.getMessage());
                }
            } catch (Exception e) {
                scheduleNextRetry(task, e.getMessage());
            }
        }
    }

    /** 指数退避：30s、60s、120s、240s */
    private void scheduleNextRetry(DonationCompensation task, String error) {
        int next = task.getRetryCount() + 1;
        if (next > MAX_RETRY) {
            markFailed(task, "超过最大重试次数：" + error);
            return;
        }
        task.setRetryCount(next);
        task.setLastError(error == null ? "未知错误" : error.substring(0, Math.min(error.length(), 480)));
        task.setNextTime(LocalDateTime.now().plusSeconds(30L * (1L << (next - 1))));
        compensationMapper.updateById(task);
        log.warn("补偿任务 {} 第 {} 次失败，{} 秒后重试：{}",
                task.getDonationNo(), next, 30L * (1L << (next - 1)),
                task.getLastError());
    }

    private void markFailed(DonationCompensation task, String error) {
        task.setStatus("FAILED");
        task.setLastError(error == null ? "未知错误" : error.substring(0, Math.min(error.length(), 480)));
        compensationMapper.updateById(task);
        log.error("补偿任务 {} 已放弃，需人工介入：{}", task.getDonationNo(), error);
    }
}
