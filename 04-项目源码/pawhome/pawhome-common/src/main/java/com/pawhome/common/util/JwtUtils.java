package com.pawhome.common.util;

import com.pawhome.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 * <p>
 * 采用 HS256 对称加密，密钥由 Nacos 配置中心下发（pawhome.jwt.secret）。
 * 网关用它验签，auth 服务用它签发，双方共识同一密钥，
 * 这样每个业务服务不必再查一次用户表就能拿到身份。
 * <p>
 * 为什么不用 Session？微服务多实例部署时 Session 需要额外做共享存储，
 * 而 JWT 自带签名，天然无状态，横向扩容不需要任何改造。
 */
@Slf4j
public class JwtUtils {

    private JwtUtils() {
    }

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_REAL_NAME = "realName";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_STATION_ID = "stationId";

    private static SecretKey buildKey(String secret) {
        // HS256 要求密钥长度不小于 256 bit，即 32 字节
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 签发 Token
     *
     * @param user     登录用户
     * @param secret   签名密钥
     * @param expireMs 有效期（毫秒）
     */
    public static String createToken(LoginUser user, String secret, long expireMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_REAL_NAME, user.getRealName())
                .claim(CLAIM_ROLE, user.getRole())
                .claim(CLAIM_STATION_ID, user.getStationId())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireMs))
                .signWith(buildKey(secret))
                .compact();
    }

    /**
     * 解析 Token，签名错误或过期都返回 null
     */
    public static LoginUser parseToken(String token, String secret) {
        if (token == null || token.isBlank()) {
            return null;
        }
        // 兼容 "Bearer xxx" 形式
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(buildKey(secret))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return LoginUser.builder()
                    .userId(Long.valueOf(claims.getSubject()))
                    .username(claims.get(CLAIM_USERNAME, String.class))
                    .realName(claims.get(CLAIM_REAL_NAME, String.class))
                    .role(claims.get(CLAIM_ROLE, String.class))
                    .stationId(claims.get(CLAIM_STATION_ID, Long.class))
                    .build();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT 解析失败：{}", e.getMessage());
            return null;
        }
    }
}
