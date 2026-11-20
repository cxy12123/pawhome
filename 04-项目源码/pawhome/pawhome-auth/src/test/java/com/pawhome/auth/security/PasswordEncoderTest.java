package com.pawhome.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 密码编码器单元测试。
 * <p>
 * 这里覆盖的是整个系统最不能出错的一段逻辑：
 * 密码一旦校验逻辑写错，要么谁都登不上，要么谁都能登。
 */
class PasswordEncoderTest {

    private final PasswordEncoder encoder = new PasswordEncoder();

    @Test
    @DisplayName("加密后能以原文校验通过，且两次密文不同（随机盐）")
    void encodeThenMatches() {
        String raw = "123456";
        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertTrue(hash1.startsWith("{bcrypt}"), "密文应带算法前缀");
        assertNotEquals(hash1, hash2, "随机盐应让同一密码产生不同密文");
        assertTrue(encoder.matches(raw, hash1));
        assertTrue(encoder.matches(raw, hash2));
    }

    @Test
    @DisplayName("错误密码和空值一律校验失败")
    void wrongPasswordFails() {
        String hash = encoder.encode("123456");

        assertFalse(encoder.matches("654321", hash));
        assertFalse(encoder.matches(null, hash));
        assertFalse(encoder.matches("123456", null));
    }

    @Test
    @DisplayName("兼容 {noop} 明文存法，并能识别需要升级的旧格式")
    void noopPrefixCompatible() {
        assertTrue(encoder.matches("123456", "{noop}123456"));
        assertTrue(encoder.needUpgrade("{noop}123456"), "明文存法必须标记为待升级");
        assertFalse(encoder.needUpgrade(encoder.encode("123456")));
    }
}
