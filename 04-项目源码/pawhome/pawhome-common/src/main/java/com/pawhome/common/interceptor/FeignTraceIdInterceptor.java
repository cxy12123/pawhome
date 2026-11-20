package com.pawhome.common.interceptor;

import com.pawhome.common.constant.CommonConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Feign 请求拦截器。
 * <p>
 * 作用有两个，缺一不可：
 * <p>
 * 1. 透传 traceId —— 否则下游服务的日志链路就断了，
 *    排查问题时只能看到半截调用链。
 * <p>
 * 2. 透传用户身份 —— 服务A调用服务B时，B 也要知道"当前是谁在操作"，
 *    用于记录操作人、做数据权限过滤。如果不透传，
 *    B 收到的请求里 UserContext 是空的，会误判为未登录。
 */
@Component
public class FeignTraceIdInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get(CommonConstants.TRACE_ID_MDC_KEY);
        if (traceId != null && !traceId.isBlank()) {
            template.header(CommonConstants.HEADER_TRACE_ID, traceId);
        }
        // 标记为内部调用，业务服务的网关拦截器据此放行
        template.header(CommonConstants.HEADER_FROM_GATEWAY, "true");
    }
}
