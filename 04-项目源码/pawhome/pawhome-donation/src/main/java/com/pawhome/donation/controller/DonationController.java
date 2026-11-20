package com.pawhome.donation.controller;

import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.donation.dto.DonationRequest;
import com.pawhome.donation.entity.Donation;
import com.pawhome.donation.service.DonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "爱心捐赠", description = "捐赠登记、支付、入库")
@RestController
@RequestMapping("/donation")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @Operation(summary = "登记捐赠单", description = "物资类捐赠需指定 supplyItemId，支付成功后自动入库")
    @PostMapping("/create")
    public R<String> create(@Valid @RequestBody DonationRequest request) {
        return R.ok("登记成功", donationService.create(request));
    }

    @Operation(summary = "模拟支付",
            description = "资金捐赠：直接置为已支付；物资捐赠：支付后调用物资服务入库，失败则进入补偿队列")
    @PostMapping("/{donationNo}/pay")
    public R<String> pay(@PathVariable String donationNo) {
        String status = donationService.pay(donationNo);
        String message = switch (status) {
            case "STORED" -> "支付成功，物资已入库";
            case "PAID" -> "支付成功";
            case "FAILED" -> "支付成功，但物资入库失败，系统将自动重试";
            default -> "处理完成";
        };
        return R.ok(message, status);
    }

    @Operation(summary = "取消捐赠单")
    @PostMapping("/{donationNo}/cancel")
    public R<Void> cancel(@PathVariable String donationNo) {
        donationService.cancel(donationNo);
        return R.ok("已取消", null);
    }

    @Operation(summary = "分页查询捐赠单")
    @GetMapping("/list")
    public R<PageResult<Donation>> page(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @Parameter(description = "MONEY/GOODS") @RequestParam(required = false) String type,
            @Parameter(description = "INIT/PAID/STORED/FAILED/CANCELED") @RequestParam(required = false) String status,
            @Parameter(description = "单号/捐赠人") @RequestParam(required = false) String keyword) {
        return R.ok(donationService.page(pageNum, pageSize, type, status, keyword));
    }

    @Operation(summary = "捐赠单详情")
    @GetMapping("/{donationNo}")
    public R<Donation> detail(@PathVariable String donationNo) {
        return R.ok(donationService.detail(donationNo));
    }

    @Operation(summary = "我的捐赠记录", description = "领养人在门户查看自己提交过的捐赠")
    @GetMapping("/my")
    public R<java.util.List<Donation>> my() {
        return R.ok(donationService.myDonations());
    }

    @Operation(summary = "捐赠统计")
    @GetMapping("/statistics")
    public R<Map<String, Object>> statistics() {
        return R.ok(donationService.statistics());
    }
}
