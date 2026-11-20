package com.pawhome.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人资料请求。
 * <p>
 * 只允许改这三个字段：账号、角色、状态属于管理侧信息，本人无权修改。
 */
@Data
@Schema(description = "修改个人资料请求")
public class ProfileUpdateRequest {

    @NotBlank(message = "姓名不能为空")
    @Size(max = 20, message = "姓名不能超过 20 个字")
    private String realName;

    @Schema(description = "手机号，可留空")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确，留空或填写 11 位手机号")
    private String phone;

    @Schema(description = "邮箱，可留空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 80, message = "邮箱过长")
    private String email;
}
