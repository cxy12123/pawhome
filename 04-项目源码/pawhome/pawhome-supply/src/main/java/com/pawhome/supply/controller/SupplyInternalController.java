package com.pawhome.supply.controller;

import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.common.core.R;
import com.pawhome.supply.service.SupplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 内部接口，供 donation 服务调用。
 */
@Tag(name = "物资服务-内部接口", description = "仅供服务间调用")
@RestController
@RequestMapping("/internal/supply")
@RequiredArgsConstructor
public class SupplyInternalController {

    private final SupplyService supplyService;

    @Operation(summary = "捐赠物资入库（幂等）",
            description = "以捐赠单号为幂等键，重复调用不会重复入库")
    @PostMapping("/inbound")
    public R<Boolean> inbound(@RequestBody SupplyInboundDTO dto) {
        return R.ok(supplyService.inboundByDonation(dto));
    }

    @Operation(summary = "查询库存")
    @GetMapping("/stock/{itemId}")
    public R<Integer> getStock(@PathVariable Long itemId) {
        return R.ok(supplyService.getStock(itemId));
    }
}
