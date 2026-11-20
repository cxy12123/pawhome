package com.pawhome.adoption.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_adoption_visit")
public class AdoptionVisit {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;
    private Integer visitNo;
    private LocalDate visitDate;
    private String visitor;
    private String animalHealth;
    private String livingEnv;

    /** PASS / WARN / FAIL */
    private String result;

    private String remark;
    private LocalDateTime createTime;
}
