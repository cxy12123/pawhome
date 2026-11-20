package com.pawhome.gateway.filter;

import com.pawhome.gateway.config.AuthProperties;
import com.pawhome.gateway.util.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 全局鉴权过滤器。
 * <p>
 * 这是整个平台的安全边界：所有外部请求必须经过这里，
 * 校验通过后把用户身份写入请求头，下游服务直接信任这些头，
 * 不再各自解析 Token（避免 8 个服务重复实现同一套逻辑）。
 * <p>
 * 一个必须注意的安全细节：
 * 先 remove 掉客户端可能伪造的 X-User-Id / X-User-Role，再写入网关解析出的真实值。
 * 否则恶意用户自己加一个 X-User-Role: ADMIN 就能越权，这是网关鉴权最常见的漏洞。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final AuthProperties authProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USER_NAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String HEADER_FROM_GATEWAY = "X-From-Gateway";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 生成或透传链路ID
        String traceId = request.getHeaders().getFirst(HEADER_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String finalTraceId = traceId;

        // 2. 白名单直接放行（登录、文档、静态资源）
        if (!authProperties.isEnabled() || isWhiteList(path)) {
            ServerHttpRequest newRequest = request.mutate()
                    .headers(h -> {
                        h.remove(HEADER_USER_ID);
                        h.remove(HEADER_USER_NAME);
                        h.remove(HEADER_USER_ROLE);
                        h.set(HEADER_FROM_GATEWAY, "true");
                        h.set(HEADER_TRACE_ID, finalTraceId);
                    })
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        // 3. 校验 Token
        String token = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        JwtHelper.Payload payload = JwtHelper.parse(token, authProperties.getSecret());
        if (payload == null) {
            log.warn("鉴权失败 path={} traceId={}", path, finalTraceId);
            return unauthorized(exchange, finalTraceId);
        }

        // 4. 角色权限矩阵：隐藏菜单只是界面体验，真正拦住越权的是这里
        if (isForbidden(path, request.getMethod().name(), payload.role)) {
            log.warn("无权限访问 path={} method={} role={} traceId={}",
                    path, request.getMethod().name(), payload.role, finalTraceId);
            return forbidden(exchange, finalTraceId);
        }

        // 5. 先清理伪造头，再写入真实身份
        String displayName = payload.realName != null ? payload.realName : payload.username;
        if (displayName == null) {
            displayName = "";
        }
        final String encodedName = URLEncoder.encode(displayName, StandardCharsets.UTF_8);
        ServerHttpRequest newRequest = request.mutate()
                .headers(h -> {
                    h.remove(HEADER_USER_ID);
                    h.remove(HEADER_USER_NAME);
                    h.remove(HEADER_USER_ROLE);
                    h.set(HEADER_USER_ID, String.valueOf(payload.userId));
                    // HTTP 头只能安全承载 ASCII，直接塞中文会被按 ISO-8859-1 编码成 '?'，
                    // 到了下游就变成 "?????"，所以这里统一 URL 编码，服务端解码还原
                    h.set(HEADER_USER_NAME, encodedName);
                    h.set(HEADER_USER_ROLE, payload.role == null ? "ADOPTER" : payload.role);
                    h.set(HEADER_TRACE_ID, finalTraceId);
                    h.set(HEADER_FROM_GATEWAY, "true");
                })
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String traceId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = String.format(
                "{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null,\"timestamp\":%d,\"traceId\":\"%s\"}",
                System.currentTimeMillis(), traceId);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** 403：已登录但角色不够 */
    private Mono<Void> forbidden(ServerWebExchange exchange, String traceId) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = String.format(
                "{\"code\":403,\"message\":\"当前角色没有该操作权限\",\"data\":null,\"timestamp\":%d,\"traceId\":\"%s\"}",
                System.currentTimeMillis(), traceId);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isWhiteList(String path) {
        return authProperties.getWhiteList().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /** 管理侧角色：管理员与救助站员工 */
    private static final java.util.Set<String> MANAGER_ROLES = java.util.Set.of("ADMIN", "STAFF");

    /**
     * 角色权限矩阵。
     * <p>
     * 返回 true 表示这个角色不允许访问该接口。
     * 前端隐藏菜单只是体验，真正防越权的是这一层 ——
     * 否则志愿者拿自己的 token 直接 curl 管理接口就能改数据。
     */
    private boolean isForbidden(String path, String method, String role) {
        String r = role == null ? "" : role;

        // 用户管理只给管理员
        if (path.startsWith("/api/auth/users")) {
            return !"ADMIN".equals(r);
        }
        // 动物档案：新增/修改/删除只给管理侧
        if (path.startsWith("/api/animal/") && isWrite(method)) {
            return !MANAGER_ROLES.contains(r);
        }
        // 领养审核、回访、全量申请列表：管理侧
        if (path.startsWith("/api/adoption/audit")
                || path.startsWith("/api/adoption/visits")
                || path.equals("/api/adoption/applies")) {
            return !MANAGER_ROLES.contains(r);
        }
        // 物资建档、出入库只给管理侧
        if (path.startsWith("/api/supply/") && isWrite(method)) {
            return !MANAGER_ROLES.contains(r);
        }
        // 捐赠全量列表给管理侧；捐赠人自己的记录走 /api/donation/my
        if (path.equals("/api/donation/list")) {
            return !MANAGER_ROLES.contains(r);
        }
        // 志愿服务：管理侧 + 志愿者
        if (path.startsWith("/api/volunteer/")) {
            return !MANAGER_ROLES.contains(r) && !"VOLUNTEER".equals(r);
        }
        return false;
    }

    private boolean isWrite(String method) {
        return "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);
    }

    @Override
    public int getOrder() {
        // 越小越先执行，鉴权必须在业务过滤器之前
        return -100;
    }
}
