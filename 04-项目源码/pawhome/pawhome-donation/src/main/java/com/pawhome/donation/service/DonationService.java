package com.pawhome.donation.service;

import com.pawhome.common.core.PageResult;
import com.pawhome.donation.dto.DonationRequest;
import com.pawhome.donation.entity.Donation;

import java.util.Map;

public interface DonationService {

    /** 登记捐赠单，返回捐赠单号 */
    String create(DonationRequest request);

    /** 模拟支付，物资类捐赠会自动触发库存入库 */
    String pay(String donationNo);

    /** 取消未支付的捐赠单 */
    void cancel(String donationNo);

    PageResult<Donation> page(long pageNum, long pageSize, String type, String status, String keyword);

    Donation detail(String donationNo);

    /** 当前登录人的捐赠记录（按时间倒序） */
    java.util.List<Donation> myDonations();

    Map<String, Object> statistics();
}
