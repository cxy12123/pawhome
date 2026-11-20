package com.pawhome.adoption.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.adoption.entity.AdoptionApply;
import com.pawhome.adoption.mapper.AdoptionApplyMapper;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.util.BizNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时把领养申请号计数器对齐数据库，避免 Redis 重启后重号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoSeedRunner {

    private final BizNoUtils bizNoUtils;
    private final AdoptionApplyMapper applyMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        bizNoUtils.seedIfLower(CommonConstants.NO_ADOPTION, date -> {
            AdoptionApply last = applyMapper.selectOne(new LambdaQueryWrapper<AdoptionApply>()
                    .likeRight(AdoptionApply::getApplyNo, CommonConstants.NO_ADOPTION + date)
                    .orderByDesc(AdoptionApply::getApplyNo)
                    .last("limit 1"));
            return BizNoUtils.tailSeq(last == null ? null : last.getApplyNo());
        });
    }
}
