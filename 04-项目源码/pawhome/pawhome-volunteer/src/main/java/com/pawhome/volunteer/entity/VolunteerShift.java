package com.pawhome.volunteer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("t_volunteer_shift")
public class VolunteerShift {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long volunteerId;
    private String volunteerName;
    private LocalDate shiftDate;

    /** HH:mm */
    private String startTime;
    private String endTime;

    /** FEED / CLEAN / ESCORT / MEDICAL / EVENT */
    private String workType;

    /** PLANNED / FINISHED / CANCELED */
    private String status;

    private String remark;
    private LocalDateTime createTime;
}
