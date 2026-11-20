package com.pawhome.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置。
 * <p>
 * secret 必须与网关 pawhome.gateway.auth.secret 完全一致，
 * 否则网关验签永远失败，表现为"登录成功但调任何接口都 401"，
 * 这是本地联调最容易踩的坑之一。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pawhome.jwt")
public class JwtProperties {

    private String secret = "pawhome-secret-key-2026-must-be-at-least-32-bytes-long";

    /** 有效期（小时） */
    private long expireHours = 24;

    /** 登录失败多少次后锁定 */
    private int maxFailCount = 5;

    /** 锁定时长（分钟） */
    private int lockMinutes = 15;

    public long getExpireMillis() {
        return expireHours * 60 * 60 * 1000;
    }
}
