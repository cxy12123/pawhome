package com.pawhome.supply.controller;

import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.supply.dto.StockChangeRequest;
import com.pawhome.supply.dto.SupplyItemVO;
import com.pawhome.supply.entity.SupplyRecord;
import com.pawhome.supply.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "物资管理", description = "物资台账、出入库、库存预警")
@RestController
@RequestMapping("/supply")
@RequiredArgsConstructor
public class SupplyController {

    private final SupplyService supplyService;

    @Operation(summary = "分页查询物资")
    @GetMapping("/items")
    public R<PageResult<SupplyItemVO>> pageItems(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "名称关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "只看库存告急") @RequestParam(required = false) Boolean onlyWarning) {
        return R.ok(supplyService.pageItems(pageNum, pageSize, keyword, categoryId, onlyWarning));
    }

    @Operation(summary = "物资详情")
    @GetMapping("/items/{id}")
    public R<SupplyItemVO> detail(@PathVariable Long id) {
        return R.ok(supplyService.detail(id));
    }

    @Operation(summary = "新增物资")
    @PostMapping("/items")
    public R<Long> create(@RequestBody SupplyItemVO vo) {
        return R.ok("新增成功", supplyService.createItem(vo));
    }

    @Operation(summary = "修改物资")
    @PutMapping("/items")
    public R<Void> update(@RequestBody SupplyItemVO vo) {
        supplyService.updateItem(vo);
        return R.ok("修改成功", null);
    }

    @Operation(summary = "物资入库", description = "使用 Redisson 分布式锁串行化，防止并发写坏库存")
    @PostMapping("/inbound")
    public R<Void> inbound(@Valid @RequestBody StockChangeRequest request) {
        supplyService.inbound(request);
        return R.ok("入库成功", null);
    }

    @Operation(summary = "物资出库",
            description = "分布式锁 + CAS 双重保护；库存不足会返回 602 错误码")
    @PostMapping("/outbound")
    public R<Void> outbound(@Valid @RequestBody StockChangeRequest request) {
        supplyService.outbound(request);
        return R.ok("出库成功", null);
    }

    @Operation(summary = "出入库流水")
    @GetMapping("/records")
    public R<PageResult<SupplyRecord>> records(
            @RequestParam(required = false) Long itemId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(supplyService.pageRecords(itemId, pageNum, pageSize));
    }

    @Operation(summary = "库存告急清单")
    @GetMapping("/warning")
    public R<List<SupplyItemVO>> warning() {
        return R.ok(supplyService.listWarning());
    }

    @Operation(summary = "物资统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(supplyService.statistics());
    }
}
