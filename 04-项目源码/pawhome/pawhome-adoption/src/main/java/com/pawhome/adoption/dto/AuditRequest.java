package com.pawhome.adoption.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "领养审核请求")
public class AuditRequest {

    @NotNull(message = "请选择申请单")
    private Long applyId;

    @NotNull(message = "请选择审核结果")
    @Schema(description = "true 通过 / false 驳回")
    private Boolean pass;

    @Schema(description = "审核意见，驳回时必填")
    private String remark;
}
