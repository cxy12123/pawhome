package com.pawhome.adoption.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "领养申请请求")
public class AdoptionApplyRequest {

    @NotNull(message = "请选择要领养的动物")
    @Schema(description = "动物ID", example = "1001")
    private Long animalId;

    @NotBlank(message = "姓名不能为空")
    private String applicantName;

    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "所在城市")
    private String city;

    @NotBlank(message = "居住地址不能为空")
    private String address;

    @Schema(description = "职业")
    private String occupation;

    @Min(value = 1, message = "家庭成员至少 1 人")
    @Schema(description = "家庭成员数")
    private Integer familyMember;

    @Schema(description = "是否有养宠经验 1/0")
    private Integer hasPetExperience;

    @Schema(description = "是否有稳定收入 1/0")
    private Integer hasStableIncome;

    @NotBlank(message = "请填写领养理由")
    private String reason;
}
