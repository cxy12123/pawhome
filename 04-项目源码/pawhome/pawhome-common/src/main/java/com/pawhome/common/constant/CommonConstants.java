package com.pawhome.common.constant;

/**
 * 全局常量。
 * <p>
 * 网关鉴权后会把这些 Header 透传给下游服务，
 * 下游服务不信任前端传入的同名 Header（网关会覆盖），
 * 这样既避免了每个服务重复解析 JWT，又保证了安全性。
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 网关透传：用户ID */
    public static final String HEADER_USER_ID = "X-User-Id";
    /** 网关透传：用户名 */
    public static final String HEADER_USER_NAME = "X-User-Name";
    /** 网关透传：角色 */
    public static final String HEADER_USER_ROLE = "X-User-Role";
    /** 链路追踪ID */
    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    /** 内部调用标记，防止外部直接访问业务服务绕过网关 */
    public static final String HEADER_FROM_GATEWAY = "X-From-Gateway";

    public static final String TRACE_ID_MDC_KEY = "traceId";

    /** Redis Key 前缀 */
    public static final String REDIS_ANIMAL_DETAIL = "pawhome:animal:detail:";
    public static final String REDIS_ANIMAL_LIST = "pawhome:animal:list:";
    public static final String REDIS_BIZ_NO = "pawhome:bizno:";
    public static final String REDIS_SUBMIT_LOCK = "pawhome:submit:";
    public static final String REDIS_TOKEN_BLACKLIST = "pawhome:token:blacklist:";

    /** 分布式锁 Key */
    public static final String LOCK_SUPPLY_STOCK = "pawhome:lock:supply:stock:";

    /** 业务单号前缀 */
    public static final String NO_ANIMAL = "PH";
    public static final String NO_ADOPTION = "AD";
    public static final String NO_DONATION = "DN";
    public static final String NO_SUPPLY_RECORD = "SR";
    public static final String NO_SUPPLY_ITEM = "SP";

    /** 角色 */
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_STAFF = "STAFF";
    public static final String ROLE_VOLUNTEER = "VOLUNTEER";
    public static final String ROLE_ADOPTER = "ADOPTER";
}
