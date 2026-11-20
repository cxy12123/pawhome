package com.pawhome.adoption.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "领养申请详情")
public class AdoptionApplyVO {

    private Long id;
    private String applyNo;
    private Long animalId;
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
    private String status;
    private String statusText;
    private String auditUserName;
    private LocalDateTime auditTime;
    private String auditRemark;
    private LocalDateTime createTime;
}
