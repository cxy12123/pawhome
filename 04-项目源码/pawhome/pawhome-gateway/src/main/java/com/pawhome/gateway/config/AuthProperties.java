package com.pawhome.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权白名单配置。
 * <p>
 * 白名单写在 yml 里而不是硬编码，好处是上线后新增公开接口
 * 只需改配置重启，不用重新打包发版。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pawhome.gateway.auth")
public class AuthProperties {

    /** JWT 签名密钥，必须与 auth 服务一致 */
    private String secret = "pawhome-secret-key-2026-must-be-at-least-32-bytes-long";

    /** 是否开启鉴权，联调阶段可临时关闭 */
    private boolean enabled = true;

    /** 免鉴权路径（支持 Ant 风格） */
    private List<String> whiteList = new ArrayList<>();
}
