package com.pawhome.supply.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.util.BizNoUtils;
import com.pawhome.supply.entity.SupplyItem;
import com.pawhome.supply.entity.SupplyRecord;
import com.pawhome.supply.mapper.SupplyItemMapper;
import com.pawhome.supply.mapper.SupplyRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时把单号计数器对齐数据库。
 * <p>
 * 本项目 Redis 未开启持久化，重启后计数器归零，
 * 生成出来的单号会和库里已有的撞车（唯一索引直接报错）。
 * 这里在应用就绪后查一次"今天已有的最大号"，把 Redis 计数器抬上去。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoSeedRunner {

    private final BizNoUtils bizNoUtils;
    private final SupplyRecordMapper recordMapper;
    private final SupplyItemMapper itemMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        bizNoUtils.seedIfLower(CommonConstants.NO_SUPPLY_RECORD, date -> {
            SupplyRecord last = recordMapper.selectOne(new LambdaQueryWrapper<SupplyRecord>()
                    .likeRight(SupplyRecord::getRecordNo, CommonConstants.NO_SUPPLY_RECORD + date)
                    .orderByDesc(SupplyRecord::getRecordNo)
                    .last("limit 1"));
            return BizNoUtils.tailSeq(last == null ? null : last.getRecordNo());
        });

        bizNoUtils.seedIfLower(CommonConstants.NO_SUPPLY_ITEM, date -> {
            SupplyItem last = itemMapper.selectOne(new LambdaQueryWrapper<SupplyItem>()
                    .likeRight(SupplyItem::getItemNo, CommonConstants.NO_SUPPLY_ITEM + date)
                    .orderByDesc(SupplyItem::getItemNo)
                    .last("limit 1"));
            return BizNoUtils.tailSeq(last == null ? null : last.getItemNo());
        });
    }
}
