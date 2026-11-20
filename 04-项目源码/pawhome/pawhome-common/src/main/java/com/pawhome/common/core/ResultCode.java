package com.pawhome.common.core;

import lombok.Getter;

/**
 * 统一响应码。
 * <p>
 * 微服务架构下每个服务独立返回，必须约定同一套码值语义，
 * 网关才能做统一兜底处理，前端也只需维护一份错误映射表。
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "登录已失效，请重新登录"),
    FORBIDDEN(403, "没有该操作权限"),
    NOT_FOUND(404, "请求的资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    BUSINESS_ERROR(600, "业务处理失败"),
    REPEAT_SUBMIT(601, "请勿重复提交"),
    STOCK_NOT_ENOUGH(602, "库存不足"),
    ANIMAL_NOT_ADOPTABLE(603, "该动物当前不可领养"),
    REMOTE_CALL_ERROR(604, "依赖服务调用失败"),
    FLOW_LIMIT(605, "当前访问人数过多，请稍后再试"),
    DEGRADE(606, "服务繁忙，已触发降级保护"),

    SYSTEM_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
