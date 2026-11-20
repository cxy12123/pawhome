package com.pawhome.api.feign.fallback;

import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.api.feign.SupplyClient;
import com.pawhome.common.core.R;
import com.pawhome.common.core.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 物资服务降级工厂。
 * <p>
 * 入库失败返回明确的错误码，donation 服务收到后
 * 会把捐赠单置为 FAILED 并写入补偿表，由定时任务重试。
 * 这就是"不引入 Seata 也能保证最终一致"的落地方式。
 */
@Slf4j
@Component
public class SupplyClientFallbackFactory implements FallbackFactory<SupplyClient> {

    @Override
    public SupplyClient create(Throwable cause) {
        log.error("调用物资服务失败：{}", cause.getMessage());
        return new SupplyClient() {
            @Override
            public R<Boolean> inbound(SupplyInboundDTO dto) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "物资入库失败，已记录待补偿");
            }

            @Override
            public R<Integer> getStock(Long itemId) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "库存查询失败");
            }
        };
    }
}
