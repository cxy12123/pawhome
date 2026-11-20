package com.pawhome.api.feign.fallback;

import com.pawhome.api.dto.AnimalDTO;
import com.pawhome.api.feign.AnimalClient;
import com.pawhome.common.core.R;
import com.pawhome.common.core.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 动物服务降级工厂。
 * <p>
 * 用 FallbackFactory 而不是 Fallback，是因为能拿到真实异常原因，
 * 排查时能区分"是超时"还是"是 404"，只用 Fallback 只能看到一句"降级了"。
 * <p>
 * 这里有一个重要的设计判断：查询类接口降级返回失败码，
 * 但领养服务拿到失败码后应直接中断流程，而不是继续往下走——
 * 否则会出现"动物信息没查到但还是生成了领养单"的脏数据。
 */
@Slf4j
@Component
public class AnimalClientFallbackFactory implements FallbackFactory<AnimalClient> {

    @Override
    public AnimalClient create(Throwable cause) {
        log.error("调用动物服务失败，已触发降级：{}", cause.getMessage());
        return new AnimalClient() {
            @Override
            public R<AnimalDTO> getById(Long id) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "动物服务暂时不可用，请稍后重试");
            }

            @Override
            public R<Boolean> updateAdoptStatus(Long id, String status) {
                return R.fail(ResultCode.REMOTE_CALL_ERROR.getCode(), "动物状态更新失败，请稍后重试");
            }

            @Override
            public R<Void> increaseView(Long id) {
                // 浏览量属于统计类数据，丢了不影响业务，直接静默降级
                return R.ok(null);
            }
        };
    }
}
