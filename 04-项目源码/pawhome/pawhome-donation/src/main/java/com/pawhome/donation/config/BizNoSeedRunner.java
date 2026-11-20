package com.pawhome.donation.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.util.BizNoUtils;
import com.pawhome.donation.entity.Donation;
import com.pawhome.donation.mapper.DonationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时把捐赠单号计数器对齐数据库，避免 Redis 重启后重号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoSeedRunner {

    private final BizNoUtils bizNoUtils;
    private final DonationMapper donationMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        bizNoUtils.seedIfLower(CommonConstants.NO_DONATION, date -> {
            Donation last = donationMapper.selectOne(new LambdaQueryWrapper<Donation>()
                    .likeRight(Donation::getDonationNo, CommonConstants.NO_DONATION + date)
                    .orderByDesc(Donation::getDonationNo)
                    .last("limit 1"));
            return BizNoUtils.tailSeq(last == null ? null : last.getDonationNo());
        });
    }
}
