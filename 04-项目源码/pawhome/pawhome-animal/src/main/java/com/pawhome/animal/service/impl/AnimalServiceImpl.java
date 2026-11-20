package com.pawhome.animal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawhome.animal.dto.AnimalSaveRequest;
import com.pawhome.animal.dto.AnimalVO;
import com.pawhome.animal.entity.Animal;
import com.pawhome.animal.entity.AnimalMedical;
import com.pawhome.animal.mapper.AnimalMapper;
import com.pawhome.animal.mapper.AnimalMedicalMapper;
import com.pawhome.animal.service.AnimalService;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.util.BizNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动物档案服务实现。
 * <p>
 * 缓存策略采用 Cache-Aside（旁路缓存）：
 * 读：先查 Redis，未命中查库并回填；写：先改库，再删缓存。
 * <p>
 * 为什么是"删缓存"而不是"更新缓存"？
 * 并发场景下两个写请求的更新顺序无法保证，可能把旧值写进缓存。
 * 删除是幂等的，最多造成一次未命中，永远不会读到脏数据。
 * <p>
 * 为什么是"先改库再删缓存"？
 * 如果先删缓存再改库，删除瞬间有读请求把旧值回填进缓存，
 * 之后库存更新完成，缓存里却是旧值，脏数据会一直留到过期。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalServiceImpl implements AnimalService {

    private final AnimalMapper animalMapper;
    private final AnimalMedicalMapper medicalMapper;
    private final StringRedisTemplate redisTemplate;
    private final BizNoUtils bizNoUtils;
    private final ObjectMapper objectMapper;

    /** 详情缓存 30 分钟，加随机偏移防止同一时刻大批 key 同时失效（缓存雪崩） */
    private static final long CACHE_SECONDS = 30 * 60L;

    private static final Map<String, String> SPECIES_TEXT = Map.of(
            "DOG", "犬", "CAT", "猫", "OTHER", "其他");
    private static final Map<String, String> GENDER_TEXT = Map.of(
            "M", "公", "F", "母");
    private static final Map<String, String> HEALTH_TEXT = Map.of(
            "HEALTHY", "健康", "TREATING", "治疗中", "RECOVERING", "康复中", "DISABLED", "残疾");
    private static final Map<String, String> ADOPT_TEXT = Map.of(
            "WAITING", "待领养", "APPLYING", "申请中", "ADOPTED", "已领养", "OBSERVING", "观察期");

    @Override
    public PageResult<AnimalVO> page(long pageNum, long pageSize, String keyword,
                                     String species, String adoptStatus, String healthStatus) {
        LambdaQueryWrapper<Animal> wrapper = new LambdaQueryWrapper<Animal>()
                .eq(StrUtil.isNotBlank(species), Animal::getSpecies, species)
                .eq(StrUtil.isNotBlank(adoptStatus), Animal::getAdoptStatus, adoptStatus)
                .eq(StrUtil.isNotBlank(healthStatus), Animal::getHealthStatus, healthStatus)
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(Animal::getName, keyword)
                        .or().like(Animal::getAnimalNo, keyword)
                        .or().like(Animal::getBreed, keyword))
                .orderByDesc(Animal::getCreateTime);

        Page<Animal> page = animalMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<AnimalVO> records = page.getRecords().stream()
                .map(a -> toVO(a, false))
                .collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public AnimalVO detail(Long id, boolean withMedical) {
        String cacheKey = CommonConstants.REDIS_ANIMAL_DETAIL + id;

        // 1. 先查缓存
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StrUtil.isNotBlank(cached)) {
                AnimalVO vo = objectMapper.readValue(cached, AnimalVO.class);
                if (!withMedical) {
                    vo.setMedicalList(null);
                }
                return vo;
            }
        } catch (Exception e) {
            // 缓存故障不能影响主流程，降级为直接查库
            log.warn("读取动物缓存失败，降级查库：{}", e.getMessage());
        }

        // 2. 缓存未命中，查库
        Animal animal = animalMapper.selectById(id);
        BizException.throwIfNull(animal, "动物不存在或已下架");

        AnimalVO vo = toVO(animal, withMedical);

        // 3. 回填缓存（带随机 TTL，防雪崩）
        try {
            long ttl = CACHE_SECONDS + new Random().nextInt(300);
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("写入动物缓存失败：{}", e.getMessage());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AnimalSaveRequest request) {
        Animal animal = new Animal();
        BeanUtil.copyProperties(request, animal);
        animal.setAnimalNo(bizNoUtils.animalNo());
        animal.setAdoptStatus("WAITING");
        animal.setViewCount(0);
        if (animal.getHealthStatus() == null) {
            animal.setHealthStatus("HEALTHY");
        }
        if (animal.getStationId() == null) {
            animal.setStationId(1L);
        }
        animalMapper.insert(animal);
        log.info("动物建档成功：{} {}", animal.getAnimalNo(), animal.getName());
        return animal.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(AnimalSaveRequest request) {
        BizException.throwIf(request.getId() == null, "修改时必须提供动物ID");
        Animal exists = animalMapper.selectById(request.getId());
        BizException.throwIfNull(exists, "动物不存在");

        Animal animal = new Animal();
        BeanUtil.copyProperties(request, animal);
        animalMapper.updateById(animal);

        // 写操作后必须删缓存，否则读到的还是旧数据
        evictCache(request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Animal exists = animalMapper.selectById(id);
        BizException.throwIfNull(exists, "动物不存在");
        BizException.throwIf("ADOPTED".equals(exists.getAdoptStatus()), "已领养的动物不能删除");
        animalMapper.deleteById(id);
        evictCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAdoptStatus(Long id, String adoptStatus) {
        Animal exists = animalMapper.selectById(id);
        BizException.throwIfNull(exists, "动物不存在");

        Animal update = new Animal();
        update.setId(id);
        update.setAdoptStatus(adoptStatus);
        animalMapper.updateById(update);

        evictCache(id);
        log.info("动物 {} 领养状态变更为 {}", exists.getAnimalNo(), adoptStatus);
        return true;
    }

    @Override
    public void increaseView(Long id) {
        // 用 SQL 原子自增，而不是先查再改，避免并发丢更新
        animalMapper.increaseView(id);
    }

    @Override
    public List<AnimalVO> listAdoptable(int limit) {
        List<Animal> list = animalMapper.selectList(new LambdaQueryWrapper<Animal>()
                .eq(Animal::getAdoptStatus, "WAITING")
                .eq(Animal::getHealthStatus, "HEALTHY")
                .orderByDesc(Animal::getCreateTime)
                .last("LIMIT " + Math.min(limit, 50)));
        return list.stream().map(a -> toVO(a, false)).collect(Collectors.toList());
    }

    @Override
    public List<AnimalVO> listWaiting(int limit) {
        // 不加健康状态过滤：治疗中/康复中的动物也展示，保证与后台"待领养"统计口径一致
        List<Animal> list = animalMapper.selectList(new LambdaQueryWrapper<Animal>()
                .eq(Animal::getAdoptStatus, "WAITING")
                .orderByDesc(Animal::getCreateTime)
                .last("LIMIT " + Math.min(limit, 50)));
        return list.stream().map(a -> toVO(a, false)).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bySpecies", toNameMap(animalMapper.countBySpecies(), "species", "total"));
        result.put("byAdoptStatus", animalMapper.countByAdoptStatus());

        Long total = animalMapper.selectCount(new LambdaQueryWrapper<Animal>());
        Long waiting = animalMapper.selectCount(new LambdaQueryWrapper<Animal>()
                .eq(Animal::getAdoptStatus, "WAITING"));
        Long adopted = animalMapper.selectCount(new LambdaQueryWrapper<Animal>()
                .eq(Animal::getAdoptStatus, "ADOPTED"));

        result.put("total", total);
        result.put("waiting", waiting);
        result.put("adopted", adopted);
        result.put("adoptRate", total == 0 ? 0 : Math.round(adopted * 1000.0 / total) / 10.0);
        result.put("today", LocalDate.now().toString());
        return result;
    }

    private Map<String, Object> toNameMap(List<Map<String, Object>> rows, String keyField, String valueField) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object key = row.get(keyField);
            map.put(String.valueOf(key), row.get(valueField));
        }
        return map;
    }

    private void evictCache(Long id) {
        try {
            redisTemplate.delete(CommonConstants.REDIS_ANIMAL_DETAIL + id);
        } catch (Exception e) {
            log.warn("删除动物缓存失败：{}", e.getMessage());
        }
    }

    private AnimalVO toVO(Animal animal, boolean withMedical) {
        AnimalVO vo = new AnimalVO();
        BeanUtil.copyProperties(animal, vo);

        vo.setSpeciesText(SPECIES_TEXT.getOrDefault(animal.getSpecies(), animal.getSpecies()));
        vo.setGenderText(GENDER_TEXT.getOrDefault(animal.getGender(), animal.getGender()));
        vo.setHealthStatusText(HEALTH_TEXT.getOrDefault(animal.getHealthStatus(), animal.getHealthStatus()));
        vo.setAdoptStatusText(ADOPT_TEXT.getOrDefault(animal.getAdoptStatus(), animal.getAdoptStatus()));
        vo.setAgeText(buildAgeText(animal.getAgeMonth()));

        if (withMedical) {
            vo.setMedicalList(medicalMapper.selectList(new LambdaQueryWrapper<AnimalMedical>()
                    .eq(AnimalMedical::getAnimalId, animal.getId())
                    .orderByDesc(AnimalMedical::getRecordDate)));
        }
        return vo;
    }

    /** 12 -> 1岁；14 -> 1岁2个月；8 -> 8个月 */
    private String buildAgeText(Integer month) {
        if (month == null || month < 0) {
            return "未知";
        }
        int year = month / 12;
        int rest = month % 12;
        if (year == 0) {
            return rest + "个月";
        }
        return rest == 0 ? year + "岁" : year + "岁" + rest + "个月";
    }
}
