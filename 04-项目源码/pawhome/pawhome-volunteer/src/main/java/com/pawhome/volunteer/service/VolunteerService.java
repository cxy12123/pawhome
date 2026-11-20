package com.pawhome.volunteer.service;

import com.pawhome.common.core.PageResult;
import com.pawhome.volunteer.entity.Volunteer;
import com.pawhome.volunteer.entity.VolunteerCheckin;
import com.pawhome.volunteer.entity.VolunteerShift;

import java.util.List;
import java.util.Map;

public interface VolunteerService {

    PageResult<Volunteer> pageVolunteers(long pageNum, long pageSize, String keyword, String level);

    Long createVolunteer(Volunteer volunteer);

    PageResult<VolunteerShift> pageShifts(long pageNum, long pageSize, String date, Long volunteerId, String status);

    Long createShift(VolunteerShift shift);

    /** 签到 */
    Long checkin(Long shiftId);

    /** 签退，返回本次服务时长 */
    double checkout(Long shiftId);

    List<VolunteerCheckin> checkinList(Long volunteerId);

    Map<String, Object> statistics();
}
