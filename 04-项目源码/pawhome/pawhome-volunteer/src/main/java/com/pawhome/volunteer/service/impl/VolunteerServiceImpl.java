package com.pawhome.volunteer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.volunteer.entity.Volunteer;
import com.pawhome.volunteer.entity.VolunteerCheckin;
import com.pawhome.volunteer.entity.VolunteerShift;
import com.pawhome.volunteer.mapper.VolunteerCheckinMapper;
import com.pawhome.volunteer.mapper.VolunteerMapper;
import com.pawhome.volunteer.mapper.VolunteerShiftMapper;
import com.pawhome.volunteer.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 志愿服务实现。
 * <p>
 * 业务规则相对简单，但打卡时长统计里有一个容易出错的点：
 * 签退时要一次性完成"写打卡记录 + 累加志愿者总时长 + 按等级升级"，
 * 这三步必须在同一个事务里，否则会出现"打卡有记录但总时长没加"的脏数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {

    private final VolunteerMapper volunteerMapper;
    private final VolunteerShiftMapper shiftMapper;
    private final VolunteerCheckinMapper checkinMapper;

    /** 等级阈值（小时） */
    private static final BigDecimal LEVEL_ACTIVE = BigDecimal.valueOf(50);
    private static final BigDecimal LEVEL_SENIOR = BigDecimal.valueOf(100);

    @Override
    public PageResult<Volunteer> pageVolunteers(long pageNum, long pageSize, String keyword, String level) {
        Page<Volunteer> page = volunteerMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Volunteer>()
                        .eq(StrUtil.isNotBlank(level), Volunteer::getLevel, level)
                        .and(StrUtil.isNotBlank(keyword), w -> w
                                .like(Volunteer::getName, keyword)
                                .or().like(Volunteer::getPhone, keyword)
                                .or().like(Volunteer::getSkills, keyword))
                        .orderByDesc(Volunteer::getTotalHours));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public Long createVolunteer(Volunteer volunteer) {
        BizException.throwIf(StrUtil.isBlank(volunteer.getName()), "姓名不能为空");
        BizException.throwIf(StrUtil.isBlank(volunteer.getPhone()), "手机号不能为空");

        Long exists = volunteerMapper.selectCount(new LambdaQueryWrapper<Volunteer>()
                .eq(Volunteer::getPhone, volunteer.getPhone()));
        if (exists != null && exists > 0) {
            throw new BizException("该手机号已登记为志愿者");
        }

        volunteer.setJoinDate(volunteer.getJoinDate() == null ? LocalDate.now() : volunteer.getJoinDate());
        volunteer.setTotalHours(BigDecimal.ZERO);
        volunteer.setLevel("ROOKIE");
        volunteer.setStatus(1);
        volunteerMapper.insert(volunteer);
        return volunteer.getId();
    }

    @Override
    public PageResult<VolunteerShift> pageShifts(long pageNum, long pageSize, String date,
                                                 Long volunteerId, String status) {
        Page<VolunteerShift> page = shiftMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<VolunteerShift>()
                        .eq(StrUtil.isNotBlank(date), VolunteerShift::getShiftDate,
                                StrUtil.isNotBlank(date) ? LocalDate.parse(date) : null)
                        .eq(volunteerId != null, VolunteerShift::getVolunteerId, volunteerId)
                        .eq(StrUtil.isNotBlank(status), VolunteerShift::getStatus, status)
                        .orderByAsc(VolunteerShift::getShiftDate)
                        .orderByAsc(VolunteerShift::getStartTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public Long createShift(VolunteerShift shift) {
        BizException.throwIf(shift.getVolunteerId() == null, "请选择志愿者");
        BizException.throwIf(shift.getShiftDate() == null, "请选择排班日期");
        BizException.throwIf(StrUtil.isBlank(shift.getStartTime()) || StrUtil.isBlank(shift.getEndTime()),
                "请填写起止时间");
        BizException.throwIf(shift.getStartTime().compareTo(shift.getEndTime()) >= 0, "结束时间必须晚于开始时间");

        Volunteer volunteer = volunteerMapper.selectById(shift.getVolunteerId());
        BizException.throwIfNull(volunteer, "志愿者不存在");

        // 同一天同一时段不允许重复排班（简单区间重叠判断）
        Long conflict = shiftMapper.selectCount(new LambdaQueryWrapper<VolunteerShift>()
                .eq(VolunteerShift::getVolunteerId, shift.getVolunteerId())
                .eq(VolunteerShift::getShiftDate, shift.getShiftDate())
                .ne(VolunteerShift::getStatus, "CANCELED")
                .le(VolunteerShift::getStartTime, shift.getEndTime())
                .ge(VolunteerShift::getEndTime, shift.getStartTime()));
        if (conflict != null && conflict > 0) {
            throw new BizException("该志愿者在此时段已有排班，请调整时间");
        }

        shift.setVolunteerName(volunteer.getName());
        shift.setStatus("PLANNED");
        shiftMapper.insert(shift);
        return shift.getId();
    }

    @Override
    public Long checkin(Long shiftId) {
        VolunteerShift shift = shiftMapper.selectById(shiftId);
        BizException.throwIfNull(shift, "排班不存在");
        if (!"PLANNED".equals(shift.getStatus())) {
            throw new BizException("该排班当前状态为「" + shift.getStatus() + "」，无法签到");
        }

        Long exists = checkinMapper.selectCount(new LambdaQueryWrapper<VolunteerCheckin>()
                .eq(VolunteerCheckin::getShiftId, shiftId)
                .isNull(VolunteerCheckin::getCheckoutTime));
        if (exists != null && exists > 0) {
            throw new BizException("已签到，请勿重复操作");
        }

        VolunteerCheckin checkin = new VolunteerCheckin();
        checkin.setShiftId(shiftId);
        checkin.setVolunteerId(shift.getVolunteerId());
        checkin.setCheckinTime(LocalDateTime.now());
        checkinMapper.insert(checkin);
        return checkin.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public double checkout(Long shiftId) {
        VolunteerCheckin checkin = checkinMapper.selectOne(new LambdaQueryWrapper<VolunteerCheckin>()
                .eq(VolunteerCheckin::getShiftId, shiftId)
                .isNull(VolunteerCheckin::getCheckoutTime)
                .orderByDesc(VolunteerCheckin::getId)
                .last("LIMIT 1"));
        BizException.throwIfNull(checkin, "未找到签到记录，请先签到");

        LocalDateTime now = LocalDateTime.now();
        double hours = Duration.between(checkin.getCheckinTime(), now).toMinutes() / 60.0;
        // 保留一位小数，最少记 0.1 小时
        BigDecimal hoursDecimal = BigDecimal.valueOf(Math.max(hours, 0.1))
                .setScale(1, RoundingMode.HALF_UP);

        checkin.setCheckoutTime(now);
        checkin.setHours(hoursDecimal);
        checkinMapper.updateById(checkin);

        // 累加志愿者总时长并升级
        Volunteer volunteer = volunteerMapper.selectById(checkin.getVolunteerId());
        BizException.throwIfNull(volunteer, "志愿者不存在");

        BigDecimal total = (volunteer.getTotalHours() == null ? BigDecimal.ZERO : volunteer.getTotalHours())
                .add(hoursDecimal);

        Volunteer update = new Volunteer();
        update.setId(volunteer.getId());
        update.setTotalHours(total);
        update.setLevel(calcLevel(total));
        volunteerMapper.updateById(update);

        // 排班置为已完成
        VolunteerShift shift = new VolunteerShift();
        shift.setId(shiftId);
        shift.setStatus("FINISHED");
        shiftMapper.updateById(shift);

        log.info("志愿者 {} 签退，本次 {} 小时，累计 {} 小时",
                volunteer.getName(), hoursDecimal, total);
        return hoursDecimal.doubleValue();
    }

    @Override
    public List<VolunteerCheckin> checkinList(Long volunteerId) {
        return checkinMapper.selectList(new LambdaQueryWrapper<VolunteerCheckin>()
                .eq(volunteerId != null, VolunteerCheckin::getVolunteerId, volunteerId)
                .orderByDesc(VolunteerCheckin::getCheckinTime));
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalVolunteers", volunteerMapper.selectCount(new LambdaQueryWrapper<Volunteer>()));

        Map<String, Long> levelMap = new LinkedHashMap<>();
        for (String level : List.of("ROOKIE", "ACTIVE", "SENIOR")) {
            levelMap.put(level, volunteerMapper.selectCount(new LambdaQueryWrapper<Volunteer>()
                    .eq(Volunteer::getLevel, level)));
        }
        result.put("byLevel", levelMap);

        BigDecimal totalHours = volunteerMapper.selectList(new LambdaQueryWrapper<Volunteer>())
                .stream()
                .map(v -> v.getTotalHours() == null ? BigDecimal.ZERO : v.getTotalHours())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("totalHours", totalHours);

        result.put("todayShifts", shiftMapper.selectCount(new LambdaQueryWrapper<VolunteerShift>()
                .eq(VolunteerShift::getShiftDate, LocalDate.now())));
        return result;
    }

    private String calcLevel(BigDecimal totalHours) {
        if (totalHours.compareTo(LEVEL_SENIOR) >= 0) {
            return "SENIOR";
        }
        if (totalHours.compareTo(LEVEL_ACTIVE) >= 0) {
            return "ACTIVE";
        }
        return "ROOKIE";
    }
}
