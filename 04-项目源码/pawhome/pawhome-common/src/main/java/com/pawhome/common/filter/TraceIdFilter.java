package com.pawhome.common.filter;

import cn.hutool.core.util.IdUtil;
import com.pawhome.common.constant.CommonConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 链路追踪过滤器。
 * <p>
 * 一次前端请求可能横跨网关 + 3~4 个微服务，
 * 出问题时想在海量日志里找线索，靠的就是同一个 traceId。
 * <p>
 * 实现思路：
 * 1. 取请求头里的 X-Trace-Id，没有就新生成一个；
 * 2. 放进 MDC，logback 的日志格式会把它打出来；
 * 3. 通过 Feign 拦截器继续传给下游服务；
 * 4. 响应头也带回去，前端报错时能直接提供 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = IdUtil.fastSimpleUUID().substring(0, 16);
        }
        try {
            MDC.put(CommonConstants.TRACE_ID_MDC_KEY, traceId);
            response.setHeader(CommonConstants.HEADER_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CommonConstants.TRACE_ID_MDC_KEY);
        }
    }
}
