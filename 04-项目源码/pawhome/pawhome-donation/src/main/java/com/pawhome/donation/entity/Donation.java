package com.pawhome.donation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_donation")
public class Donation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String donationNo;
    private Long donorId;
    private String donorName;
    private String donorPhone;

    /** PERSON / COMPANY */
    private String donorType;

    /** MONEY / GOODS */
    private String type;

    private BigDecimal amount;
    private String itemDesc;
    private Long supplyItemId;
    private Integer quantity;
    private String remark;

    /** INIT / PAID / STORED / FAILED / CANCELED */
    private String status;

    private LocalDateTime payTime;
    private LocalDateTime storeTime;
    private Integer retryCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
