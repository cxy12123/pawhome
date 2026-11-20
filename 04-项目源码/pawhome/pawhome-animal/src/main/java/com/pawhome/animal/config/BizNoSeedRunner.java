package com.pawhome.animal.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.animal.entity.Animal;
import com.pawhome.animal.mapper.AnimalMapper;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.util.BizNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动时把动物档案号计数器对齐数据库，避免 Redis 重启后重号。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BizNoSeedRunner {

    private final BizNoUtils bizNoUtils;
    private final AnimalMapper animalMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        bizNoUtils.seedIfLower(CommonConstants.NO_ANIMAL, date -> {
            Animal last = animalMapper.selectOne(new LambdaQueryWrapper<Animal>()
                    .likeRight(Animal::getAnimalNo, CommonConstants.NO_ANIMAL + date)
                    .orderByDesc(Animal::getAnimalNo)
                    .last("limit 1"));
            return BizNoUtils.tailSeq(last == null ? null : last.getAnimalNo());
        });
    }
}
