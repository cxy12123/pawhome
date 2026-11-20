package com.pawhome.volunteer.controller;

import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.volunteer.entity.Volunteer;
import com.pawhome.volunteer.entity.VolunteerCheckin;
import com.pawhome.volunteer.entity.VolunteerShift;
import com.pawhome.volunteer.service.VolunteerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "志愿服务", description = "志愿者管理、排班、打卡")
@RestController
@RequestMapping("/volunteer")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @Operation(summary = "分页查询志愿者")
    @GetMapping("/volunteers")
    public R<PageResult<Volunteer>> pageVolunteers(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "姓名/手机号/技能") @RequestParam(required = false) String keyword,
            @Parameter(description = "ROOKIE/ACTIVE/SENIOR") @RequestParam(required = false) String level) {
        return R.ok(volunteerService.pageVolunteers(pageNum, pageSize, keyword, level));
    }

    @Operation(summary = "登记志愿者")
    @PostMapping("/volunteers")
    public R<Long> createVolunteer(@RequestBody Volunteer volunteer) {
        return R.ok("登记成功", volunteerService.createVolunteer(volunteer));
    }

    @Operation(summary = "分页查询排班")
    @GetMapping("/shifts")
    public R<PageResult<VolunteerShift>> pageShifts(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "日期 yyyy-MM-dd") @RequestParam(required = false) String date,
            @RequestParam(required = false) Long volunteerId,
            @Parameter(description = "PLANNED/FINISHED/CANCELED") @RequestParam(required = false) String status) {
        return R.ok(volunteerService.pageShifts(pageNum, pageSize, date, volunteerId, status));
    }

    @Operation(summary = "新增排班", description = "会自动检查同时段冲突")
    @PostMapping("/shifts")
    public R<Long> createShift(@RequestBody VolunteerShift shift) {
        return R.ok("排班成功", volunteerService.createShift(shift));
    }

    @Operation(summary = "签到")
    @PostMapping("/shifts/{id}/checkin")
    public R<Long> checkin(@PathVariable Long id) {
        return R.ok("签到成功", volunteerService.checkin(id));
    }

    @Operation(summary = "签退", description = "自动计算时长、累加总工时并更新志愿者等级")
    @PostMapping("/shifts/{id}/checkout")
    public R<String> checkout(@PathVariable Long id) {
        double hours = volunteerService.checkout(id);
        return R.ok(String.format("签退成功，本次服务 %.1f 小时", hours), null);
    }

    @Operation(summary = "打卡记录")
    @GetMapping("/checkins")
    public R<List<VolunteerCheckin>> checkins(@RequestParam(required = false) Long volunteerId) {
        return R.ok(volunteerService.checkinList(volunteerId));
    }

    @Operation(summary = "志愿服务统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(volunteerService.statistics());
    }
}
