package com.pawhome.common.context;

import com.pawhome.common.core.BizException;
import com.pawhome.common.core.ResultCode;

/**
 * 用户上下文。
 * <p>
 * 网关解析 JWT 后把用户信息放进 Header，下游服务由拦截器写入 ThreadLocal，
 * Service 层通过 UserContext.getUserId() 直接取，不必层层传参。
 * <p>
 * 注意：使用 ThreadLocal 必须在请求结束时 remove()，否则线程池复用会造成
 * 用户信息串号（这是生产事故的经典来源），本类在拦截器 afterCompletion 中处理。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUserId();
    }

    public static String getUsername() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getUsername();
    }

    public static String getRealName() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRealName();
    }

    public static String getRole() {
        LoginUser user = HOLDER.get();
        return user == null ? null : user.getRole();
    }

    /** 取当前用户ID，取不到直接抛 401，用于必须登录的接口 */
    public static Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
