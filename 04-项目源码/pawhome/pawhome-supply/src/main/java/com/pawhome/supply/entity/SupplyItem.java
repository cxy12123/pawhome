package com.pawhome.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_supply_item")
public class SupplyItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String itemNo;
    private String name;
    private Long categoryId;
    private String categoryName;
    private String unit;
    private String spec;

    /** 当前库存 */
    private Integer stock;

    /** 预警阈值 */
    private Integer warnStock;

    private BigDecimal price;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
