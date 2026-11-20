package com.pawhome.supply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "物资信息")
public class SupplyItemVO {

    private Long id;
    private String itemNo;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String unit;
    private String spec;
    private Integer stock;
    private Integer warnStock;
    private BigDecimal price;
    private Integer status;
    private LocalDateTime updateTime;

    @Schema(description = "是否库存告急")
    private Boolean warning;
}
