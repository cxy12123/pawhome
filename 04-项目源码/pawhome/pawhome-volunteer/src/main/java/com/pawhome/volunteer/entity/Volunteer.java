package com.pawhome.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_volunteer")
public class Volunteer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;
    private String name;
    private String phone;
    private String gender;
    private String skills;
    private LocalDate joinDate;

    /** 累计服务时长 */
    private BigDecimal totalHours;

    /** ROOKIE / ACTIVE / SENIOR */
    private String level;

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
