package com.pawhome.common.core;

import lombok.Getter;

/**
 * 业务异常。
 * <p>
 * 所有可预期的业务失败都抛这个异常，由 GlobalExceptionHandler 统一转换成 R，
 * 避免每个 Controller 里写一堆 if-else 拼返回体。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /** 条件成立时抛出异常，用来替代大量的 if 判空 */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new BizException(message);
        }
    }

    public static void throwIfNull(Object obj, String message) {
        if (obj == null) {
            throw new BizException(ResultCode.NOT_FOUND, message);
        }
    }
}
