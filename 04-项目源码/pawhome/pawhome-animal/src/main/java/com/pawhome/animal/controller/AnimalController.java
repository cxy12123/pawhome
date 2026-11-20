package com.pawhome.animal.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.animal.dto.AnimalSaveRequest;
import com.pawhome.animal.dto.AnimalVO;
import com.pawhome.animal.entity.AnimalMedical;
import com.pawhome.animal.mapper.AnimalMedicalMapper;
import com.pawhome.animal.service.AnimalService;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 动物档案接口（经网关对外暴露）。
 */
@Tag(name = "动物档案", description = "动物建档、查询、状态维护")
@RestController
@RequestMapping("/animal")
@RequiredArgsConstructor
public class AnimalController {

    private final AnimalService animalService;
    private final AnimalMedicalMapper medicalMapper;

    @Operation(summary = "分页查询动物")
    @GetMapping("/animals")
    public R<PageResult<AnimalVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "关键字：昵称/编号/品种") @RequestParam(required = false) String keyword,
            @Parameter(description = "物种 DOG/CAT/OTHER") @RequestParam(required = false) String species,
            @Parameter(description = "领养状态") @RequestParam(required = false) String adoptStatus,
            @Parameter(description = "健康状态") @RequestParam(required = false) String healthStatus) {
        return R.ok(animalService.page(pageNum, pageSize, keyword, species, adoptStatus, healthStatus));
    }

    @Operation(summary = "动物详情", description = "带 Redis 缓存，第二次访问直接命中缓存")
    @GetMapping("/animals/{id}")
    public R<AnimalVO> detail(@PathVariable Long id,
                              @RequestParam(defaultValue = "true") boolean withMedical) {
        return R.ok(animalService.detail(id, withMedical));
    }

    @Operation(summary = "新增动物档案")
    @PostMapping("/animals")
    public R<Long> create(@Valid @RequestBody AnimalSaveRequest request) {
        return R.ok("建档成功", animalService.create(request));
    }

    @Operation(summary = "修改动物档案")
    @PutMapping("/animals")
    public R<Void> update(@Valid @RequestBody AnimalSaveRequest request) {
        animalService.update(request);
        return R.ok("修改成功", null);
    }

    @Operation(summary = "删除动物档案")
    @DeleteMapping("/animals/{id}")
    public R<Void> delete(@PathVariable Long id) {
        animalService.delete(id);
        return R.ok("删除成功", null);
    }

    @Operation(summary = "诊疗记录列表")
    @GetMapping("/animals/{id}/medical")
    public R<List<AnimalMedical>> medicalList(@PathVariable Long id) {
        return R.ok(medicalMapper.selectList(
                new LambdaQueryWrapper<AnimalMedical>()
                        .eq(AnimalMedical::getAnimalId, id)
                        .orderByDesc(AnimalMedical::getRecordDate)));
    }

    @Operation(summary = "新增诊疗记录")
    @PostMapping("/medical")
    public R<Long> addMedical(@RequestBody AnimalMedical medical) {
        BizException.throwIf(medical.getAnimalId() == null, "请指定动物");
        BizException.throwIf(medical.getTitle() == null, "请填写记录标题");
        medicalMapper.insert(medical);
        return R.ok("记录已保存", medical.getId());
    }

    @Operation(summary = "待领养动物列表", description = "公开接口，无需登录")
    @GetMapping("/public/adoptable")
    public R<List<AnimalVO>> adoptable(@RequestParam(defaultValue = "12") int limit) {
        return R.ok(animalService.listAdoptable(limit));
    }

    @Operation(summary = "在站待领养动物（含治疗中）",
            description = "公开接口。与 /public/adoptable 的区别是不做健康状态过滤，"
                    + "用于门户展示与后台统计保持同一口径，具体能否申请由前端按 healthStatus 判断")
    @GetMapping("/public/waiting")
    public R<List<AnimalVO>> waiting(@RequestParam(defaultValue = "60") int limit) {
        return R.ok(animalService.listWaiting(limit));
    }

    @Operation(summary = "统计看板")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(animalService.statistics());
    }
}
