package com.pawhome.common.interceptor;

import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.context.LoginUser;
import com.pawhome.common.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器。
 * <p>
 * 网关完成 JWT 校验后，会把 userId / username / role 写进请求头。
 * 这里把它们还原成 LoginUser 放进 ThreadLocal，
 * 业务代码就可以直接 UserContext.getUserId()，不用每个方法都传一遍。
 * <p>
 * afterCompletion 必须清理 ThreadLocal：Tomcat 线程是复用的，
 * 不清理会导致下一个请求读到上一个用户的身份，属于严重安全事故。
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            String name = decode(request.getHeader(CommonConstants.HEADER_USER_NAME));
            UserContext.set(LoginUser.builder()
                    .userId(Long.valueOf(userId))
                    .username(name)
                    .realName(name)
                    .role(request.getHeader(CommonConstants.HEADER_USER_ROLE))
                    .build());
        }
        return true;
    }

    /**
     * 网关写入的中文姓名做了 URL 编码（HTTP 头按 ISO-8859-1 传字节，中文会被压成 '?'）。
     * 这里解码还原；不含 % 的值直接返回，兼容老版本和纯英文账号。
     */
    private String decode(String value) {
        if (value == null || value.indexOf('%') < 0) {
            return value;
        }
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
