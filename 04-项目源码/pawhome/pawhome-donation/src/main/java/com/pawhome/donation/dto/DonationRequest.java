package com.pawhome.donation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "捐赠登记请求")
public class DonationRequest {

    @NotBlank(message = "捐赠人名称不能为空")
    private String donorName;

    @Schema(description = "联系电话")
    private String donorPhone;

    @Schema(description = "捐赠主体：PERSON/COMPANY")
    private String donorType;

    @NotBlank(message = "请选择捐赠类型")
    @Schema(description = "MONEY 资金 / GOODS 物资")
    private String type;

    @Schema(description = "金额，MONEY 类型必填")
    private BigDecimal amount;

    @Schema(description = "物资描述，GOODS 类型必填")
    private String itemDesc;

    @Schema(description = "对应物资ID，GOODS 类型必填，用于入库")
    private Long supplyItemId;

    @Schema(description = "物资数量")
    private Integer quantity;

    private String remark;
}
