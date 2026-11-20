package com.pawhome.supply.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出入库流水。
 * <p>
 * 这张表只增不改，是库存变更的唯一凭证。
 * 台账表的 stock 字段只是流水累计的结果，出现差异时以流水为准重算。
 */
@Data
@TableName("t_supply_record")
public class SupplyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;
    private Long itemId;
    private String itemName;

    /** IN 入库 / OUT 出库 */
    private String type;

    private Integer quantity;
    private Integer beforeStock;
    private Integer afterStock;

    /** DONATION / PURCHASE / ISSUE / SCRAP / MANUAL */
    private String sourceType;

    private Long sourceId;
    private Long operatorId;
    private String operatorName;
    private String remark;
    private LocalDateTime createTime;
}
