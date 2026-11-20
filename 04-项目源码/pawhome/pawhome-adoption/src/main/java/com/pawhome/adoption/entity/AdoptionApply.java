package com.pawhome.adoption.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_adoption_apply")
public class AdoptionApply {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String applyNo;

    /** 逻辑外键，指向 animal 服务的动物ID，本库不建外键约束 */
    private Long animalId;

    /** 冗余快照：避免列表查询时跨服务调用 */
    private String animalNo;
    private String animalName;

    private Long applicantId;
    private String applicantName;
    private String phone;
    private String city;
    private String address;
    private String occupation;
    private Integer familyMember;
    private Integer hasPetExperience;
    private Integer hasStableIncome;
    private String reason;

    /** PENDING / PASSED / REJECTED / CANCELED / FINISHED */
    private String status;

    private Long auditUserId;
    private String auditUserName;
    private LocalDateTime auditTime;
    private String auditRemark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
