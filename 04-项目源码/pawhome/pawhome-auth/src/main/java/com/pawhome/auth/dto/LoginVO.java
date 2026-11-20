package com.pawhome.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "登录响应")
public class LoginVO {

    @Schema(description = "JWT 令牌，后续请求放在 Authorization 头中，格式 Bearer {token}")
    private String token;

    @Schema(description = "令牌类型")
    private String tokenType;

    @Schema(description = "有效期（秒）")
    private Long expiresIn;

    @Schema(description = "用户信息")
    private UserVO user;
}
