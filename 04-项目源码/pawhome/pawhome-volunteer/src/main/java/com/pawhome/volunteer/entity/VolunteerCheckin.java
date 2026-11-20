package com.pawhome.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_volunteer_checkin")
public class VolunteerCheckin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shiftId;
    private Long volunteerId;
    private LocalDateTime checkinTime;
    private LocalDateTime checkoutTime;
    private BigDecimal hours;
    private String remark;
    private LocalDateTime createTime;
}
