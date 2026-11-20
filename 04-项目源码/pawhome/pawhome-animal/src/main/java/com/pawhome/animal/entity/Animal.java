package com.pawhome.animal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_animal")
public class Animal {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String animalNo;
    private String name;

    /** DOG / CAT / OTHER */
    private String species;

    private String breed;

    /** M 公 / F 母 */
    private String gender;

    private Integer ageMonth;
    private BigDecimal weight;

    /** HEALTHY / TREATING / RECOVERING / DISABLED */
    private String healthStatus;

    /** WAITING / APPLYING / ADOPTED / OBSERVING */
    private String adoptStatus;

    private LocalDate rescueDate;
    private String rescueCity;
    private String rescueSource;
    private Long stationId;
    private Integer sterilized;
    private Integer vaccinated;
    private Integer dewormed;
    private String description;
    private String coverImage;
    private Integer viewCount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
