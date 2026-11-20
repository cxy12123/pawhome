package com.pawhome.auth.security;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 密码编码器。
 * <p>
 * 采用"前缀标识算法"的委托式设计（参考 Spring Security 的 DelegatingPasswordEncoder）：
 * <pre>
 *   {noop}123456          明文，仅用于初始化数据，登录成功后立即升级
 *   {bcrypt}$2a$10$...    BCrypt 密文，正式存储格式
 * </pre>
 * <p>
 * 为什么用 BCrypt 而不是 MD5/SHA256：
 * 1. BCrypt 内置随机盐，同一个密码每次加密结果都不同，彩虹表失效；
 * 2. cost 参数可调，计算耗时约 100ms，暴力破解成本极高；
 * 3. MD5 已被证明不安全，且不加盐时可直接查表反推。
 * <p>
 * 为什么不加盐 MD5？因为盐要和密码一起存，一旦库被拖走，
 * 攻击者拿到盐照样能批量撞，而 BCrypt 的慢哈希才是根本防护。
 */
@Slf4j
@Component
public class PasswordEncoder {

    private static final String NOOP_PREFIX = "{noop}";
    private static final String BCRYPT_PREFIX = "{bcrypt}";

    /** 加密，统一输出 {bcrypt} 前缀格式 */
    public String encode(String rawPassword) {
        return BCRYPT_PREFIX + BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 校验密码，兼容三种存储形态
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        try {
            if (storedPassword.startsWith(NOOP_PREFIX)) {
                return storedPassword.substring(NOOP_PREFIX.length()).equals(rawPassword);
            }
            if (storedPassword.startsWith(BCRYPT_PREFIX)) {
                return BCrypt.checkpw(rawPassword, storedPassword.substring(BCRYPT_PREFIX.length()));
            }
            // 兼容历史无前缀的 BCrypt 密文
            if (storedPassword.startsWith("$2")) {
                return BCrypt.checkpw(rawPassword, storedPassword);
            }
            return false;
        } catch (Exception e) {
            log.warn("密码校验异常：{}", e.getMessage());
            return false;
        }
    }

    /** 是否需要升级（明文或旧算法都要升级） */
    public boolean needUpgrade(String storedPassword) {
        return storedPassword == null || !storedPassword.startsWith(BCRYPT_PREFIX);
    }
}
