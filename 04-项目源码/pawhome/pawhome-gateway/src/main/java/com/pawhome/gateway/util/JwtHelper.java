package com.pawhome.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * 网关侧 JWT 解析工具。
 * <p>
 * 网关是 WebFlux 技术栈，不能依赖基于 Servlet 的 pawhome-common，
 * 所以这里单独实现一份最小解析逻辑。这也是网关模块唯一的"重复代码"，
 * 换来的是技术栈隔离。
 */
@Slf4j
public final class JwtHelper {

    private JwtHelper() {
    }

    public static class Payload {
        public Long userId;
        public String username;
        public String realName;
        public String role;

        public Payload(Long userId, String username, String realName, String role) {
            this.userId = userId;
            this.username = username;
            this.realName = realName;
            this.role = role;
        }
    }

    /**
     * 验签并解析，失败返回 null（过期、篡改、格式错都算失败）
     */
    public static Payload parse(String token, String secret) {
        if (token == null || token.isBlank()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new Payload(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("realName", String.class),
                    claims.get("role", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("网关 JWT 校验失败：{}", e.getMessage());
            return null;
        }
    }
}
