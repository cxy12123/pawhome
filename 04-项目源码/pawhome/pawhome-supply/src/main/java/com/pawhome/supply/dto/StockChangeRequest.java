package com.pawhome.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "出入库请求")
public class StockChangeRequest {

    @NotNull(message = "请选择物资")
    private Long itemId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;

    @Schema(description = "来源：DONATION/PURCHASE/ISSUE/SCRAP/MANUAL")
    private String sourceType;

    @Schema(description = "关联单据ID")
    private Long sourceId;

    @Schema(description = "备注")
    private String remark;
}
