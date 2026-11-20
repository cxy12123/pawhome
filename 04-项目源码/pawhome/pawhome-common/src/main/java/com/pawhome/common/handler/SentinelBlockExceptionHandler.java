package com.pawhome.common.handler;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawhome.common.core.R;
import com.pawhome.common.core.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Sentinel 全局限流/降级处理器。
 * <p>
 * 默认情况下被 Sentinel 拦下的请求会抛出一个带英文消息的异常，
 * 前端拿到的是 500 加一堆看不懂的文字。这里统一改写成标准 R 结构，
 * 并按不同拦截原因给出可读提示：
 * <p>
 * - FlowException       触发流控（QPS 超阈值）
 * - DegradeException    触发熔断（下游错误率过高，主动拒绝一段时间）
 * - ParamFlowException  热点参数限流（比如同一个用户狂刷某接口）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelBlockExceptionHandler implements BlockExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        log.warn("Sentinel 拦截请求 uri={} rule={}", request.getRequestURI(), e.getRule());

        R<Void> body;
        if (e instanceof FlowException) {
            body = R.fail(ResultCode.FLOW_LIMIT.getCode(), "当前访问过于频繁，请稍后再试");
        } else if (e instanceof DegradeException) {
            body = R.fail(ResultCode.DEGRADE.getCode(), "服务暂时不可用（已熔断保护），请稍后重试");
        } else if (e instanceof ParamFlowException) {
            body = R.fail(ResultCode.FLOW_LIMIT.getCode(), "操作过于频繁，请 1 分钟后再试");
        } else {
            body = R.fail(ResultCode.DEGRADE.getCode(), "系统繁忙，请稍后再试");
        }

        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
