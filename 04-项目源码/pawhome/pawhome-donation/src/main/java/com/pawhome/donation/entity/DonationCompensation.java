package com.pawhome.donation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 补偿任务实体。
 */
@Data
@TableName("t_donation_compensation")
public class DonationCompensation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long donationId;
    private String donationNo;

    /** SUPPLY_IN 物资入库 */
    private String action;

    /** WAITING / SUCCESS / FAILED */
    private String status;

    private Integer retryCount;
    private String lastError;
    private LocalDateTime nextTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
