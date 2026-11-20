package com.pawhome.animal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_animal_medical")
public class AnimalMedical {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long animalId;

    /** VACCINE / TREATMENT / STERILIZE / CHECK */
    private String recordType;

    private String title;
    private String vetName;
    private BigDecimal cost;
    private LocalDate recordDate;
    private String remark;
    private LocalDateTime createTime;
}
