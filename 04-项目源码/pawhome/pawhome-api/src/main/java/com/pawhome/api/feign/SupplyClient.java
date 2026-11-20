package com.pawhome.api.feign;

import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.api.feign.fallback.SupplyClientFallbackFactory;
import com.pawhome.common.core.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 物资服务 Feign 客户端。
 * <p>
 * 物资入库是捐赠流程的关键一环，失败必须让调用方感知，
 * 因此 fallback 返回失败码，由 donation 服务记录补偿任务重试。
 */
@FeignClient(
        name = "pawhome-supply",
        contextId = "supplyClient",
        path = "/internal/supply",
        fallbackFactory = SupplyClientFallbackFactory.class
)
public interface SupplyClient {

    /** 物资入库（幂等） */
    @PostMapping("/inbound")
    R<Boolean> inbound(@RequestBody SupplyInboundDTO dto);

    /** 查询物资当前库存 */
    @GetMapping("/stock/{itemId}")
    R<Integer> getStock(@PathVariable("itemId") Long itemId);
}
