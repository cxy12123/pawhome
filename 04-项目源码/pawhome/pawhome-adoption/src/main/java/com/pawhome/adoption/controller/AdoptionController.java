package com.pawhome.adoption.controller;

import com.pawhome.adoption.dto.AdoptionApplyRequest;
import com.pawhome.adoption.dto.AdoptionApplyVO;
import com.pawhome.adoption.dto.AuditRequest;
import com.pawhome.adoption.entity.AdoptionVisit;
import com.pawhome.adoption.service.AdoptionService;
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

@Tag(name = "领养管理", description = "申请、审核、回访")
@RestController
@RequestMapping("/adoption")
@RequiredArgsConstructor
public class AdoptionController {

    private final AdoptionService adoptionService;

    @Operation(summary = "提交领养申请",
            description = "会通过 Feign 调用动物服务校验动物是否处于可领养状态，校验通过后把动物置为「申请中」")
    @PostMapping("/apply")
    public R<String> apply(@Valid @RequestBody AdoptionApplyRequest request) {
        return R.ok("申请已提交，请等待审核", adoptionService.submit(request));
    }

    @Operation(summary = "审核领养申请",
            description = "通过：动物状态改为已领养；驳回：动物状态放回待领养。仅 STAFF/ADMIN 可操作")
    @PostMapping("/audit")
    public R<Void> audit(@Valid @RequestBody AuditRequest request) {
        adoptionService.audit(request);
        return R.ok(Boolean.TRUE.equals(request.getPass()) ? "已通过审核" : "已驳回", null);
    }

    @Operation(summary = "取消申请")
    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        adoptionService.cancel(id, reason);
        return R.ok("已取消", null);
    }

    @Operation(summary = "分页查询申请单")
    @GetMapping("/applies")
    public R<PageResult<AdoptionApplyVO>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "PENDING/PASSED/REJECTED/CANCELED") @RequestParam(required = false) String status,
            @Parameter(description = "单号/申请人/动物名") @RequestParam(required = false) String keyword,
            @Parameter(description = "按申请人过滤") @RequestParam(required = false) Long applicantId) {
        return R.ok(adoptionService.page(pageNum, pageSize, status, keyword, applicantId));
    }

    @Operation(summary = "申请单详情")
    @GetMapping("/applies/{id}")
    public R<AdoptionApplyVO> detail(@PathVariable Long id) {
        return R.ok(adoptionService.detail(id));
    }

    @Operation(summary = "我的领养申请")
    @GetMapping("/my")
    public R<List<AdoptionApplyVO>> my() {
        return R.ok(adoptionService.myApplications());
    }

    @Operation(summary = "新增回访记录")
    @PostMapping("/visits")
    public R<Void> addVisit(@RequestBody AdoptionVisit visit) {
        adoptionService.addVisit(visit);
        return R.ok("回访记录已保存", null);
    }

    @Operation(summary = "回访记录列表")
    @GetMapping("/visits/{applyId}")
    public R<List<AdoptionVisit>> visits(@PathVariable Long applyId) {
        return R.ok(adoptionService.visitList(applyId));
    }

    @Operation(summary = "领养统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(adoptionService.statistics());
    }
}
