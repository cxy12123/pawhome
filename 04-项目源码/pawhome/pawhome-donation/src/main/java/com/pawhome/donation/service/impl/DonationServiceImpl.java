package com.pawhome.donation.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.api.feign.NotifyClient;
import com.pawhome.api.feign.SupplyClient;
import com.pawhome.common.context.UserContext;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.common.core.R;
import com.pawhome.common.util.BizNoUtils;
import com.pawhome.donation.dto.DonationRequest;
import com.pawhome.donation.entity.Donation;
import com.pawhome.donation.entity.DonationCompensation;
import com.pawhome.donation.mapper.DonationCompensationMapper;
import com.pawhome.donation.mapper.DonationMapper;
import com.pawhome.donation.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 捐赠服务实现 —— 分布式一致性的落地示范。
 * <p>
 * 关键场景：用户捐物资，支付成功（本地事务已提交），
 * 但调用 supply 服务入库时网络超时。
 * <p>
 * 此时如果直接抛异常回滚，会出现"钱已收、单据没了"的问题；
 * 如果不管，则"钱收了、物资没入库"，账实不符。
 * <p>
 * 本项目的解法：
 * <pre>
 *   1. 先提交本地事务（捐赠单置为 PAID）；
 *   2. 再发起远程调用；
 *   3. 调用失败不抛异常，把单据标为 FAILED，并写入补偿表；
 *   4. 定时任务每 30 秒扫描补偿表重试，直到成功或超过 5 次；
 *   5. supply 侧的入库接口是幂等的，重复调用不会重复入库。
 * </pre>
 * 这套组合拳保证了"最终一致"，且不需要引入 Seata 的 TC 组件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationMapper donationMapper;
    private final DonationCompensationMapper compensationMapper;
    private final SupplyClient supplyClient;
    private final NotifyClient notifyClient;
    private final BizNoUtils bizNoUtils;

    @Override
    public String create(DonationRequest request) {
        if ("MONEY".equals(request.getType())) {
            BizException.throwIf(request.getAmount() == null
                    || request.getAmount().compareTo(BigDecimal.ZERO) <= 0, "资金捐赠必须填写有效金额");
        } else if ("GOODS".equals(request.getType())) {
            BizException.throwIf(StrUtil.isBlank(request.getItemDesc()), "物资捐赠请填写物资描述");
            BizException.throwIf(request.getSupplyItemId() == null, "请选择要捐赠的物资，以便入库");
            BizException.throwIf(request.getQuantity() == null || request.getQuantity() <= 0, "请填写捐赠数量");
        } else {
            throw new BizException("捐赠类型只能为 MONEY 或 GOODS");
        }

        Donation donation = new Donation();
        donation.setDonationNo(bizNoUtils.donationNo());
        donation.setDonorId(UserContext.getUserId());
        donation.setDonorName(request.getDonorName());
        donation.setDonorPhone(request.getDonorPhone());
        donation.setDonorType(StrUtil.isBlank(request.getDonorType()) ? "PERSON" : request.getDonorType());
        donation.setType(request.getType());
        donation.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        donation.setItemDesc(request.getItemDesc());
        donation.setSupplyItemId(request.getSupplyItemId());
        donation.setQuantity(request.getQuantity() == null ? 0 : request.getQuantity());
        donation.setRemark(request.getRemark());
        donation.setStatus("INIT");
        donation.setRetryCount(0);
        donationMapper.insert(donation);

        log.info("捐赠单已创建：{} 类型 {} ", donation.getDonationNo(), donation.getType());
        return donation.getDonationNo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String pay(String donationNo) {
        Donation donation = donationMapper.selectOne(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getDonationNo, donationNo));
        BizException.throwIfNull(donation, "捐赠单不存在");

        if (!"INIT".equals(donation.getStatus())) {
            throw new BizException("当前状态为「" + donation.getStatus() + "」，不可支付");
        }

        // ---------- 1. 本地事务：置为已支付 ----------
        Donation paid = new Donation();
        paid.setId(donation.getId());
        paid.setStatus("PAID");
        paid.setPayTime(LocalDateTime.now());
        donationMapper.updateById(paid);

        // 资金捐赠到此结束，不需要入库
        if ("MONEY".equals(donation.getType())) {
            sendNotify(donation, "感谢您的爱心捐赠",
                    String.format("您好 %s，您捐赠的 ¥%s 已到账，将全部用于流浪动物救助。",
                            donation.getDonorName(), donation.getAmount()));
            return "PAID";
        }

        // ---------- 2. 物资捐赠：调用 supply 服务入库 ----------
        boolean stored = tryInbound(donation);

        if (stored) {
            Donation done = new Donation();
            done.setId(donation.getId());
            done.setStatus("STORED");
            done.setStoreTime(LocalDateTime.now());
            donationMapper.updateById(done);

            sendNotify(donation, "感谢您的捐赠",
                    String.format("您好 %s，您捐赠的「%s」×%d 已入库，感谢您对流浪动物的关爱。",
                            donation.getDonorName(), donation.getItemDesc(), donation.getQuantity()));
            return "STORED";
        }

        // ---------- 3. 入库失败：标记失败 + 写补偿任务 ----------
        Donation failed = new Donation();
        failed.setId(donation.getId());
        failed.setStatus("FAILED");
        failed.setRetryCount(1);
        donationMapper.updateById(failed);

        DonationCompensation task = new DonationCompensation();
        task.setDonationId(donation.getId());
        task.setDonationNo(donation.getDonationNo());
        task.setAction("SUPPLY_IN");
        task.setStatus("WAITING");
        task.setRetryCount(1);
        task.setLastError("首次入库调用失败");
        task.setNextTime(LocalDateTime.now().plusSeconds(30));
        compensationMapper.insert(task);

        log.warn("捐赠单 {} 物资入库失败，已记录补偿任务", donation.getDonationNo());
        return "FAILED";
    }

    /**
     * 尝试入库。
     * 这里刻意捕获所有异常而不外抛 —— 因为此时本地事务必须提交，
     * 否则"已支付"的事实就丢了，补偿也就无从谈起。
     */
    private boolean tryInbound(Donation donation) {
        try {
            R<Boolean> resp = supplyClient.inbound(SupplyInboundDTO.builder()
                    .requestId(donation.getDonationNo())
                    .itemId(donation.getSupplyItemId())
                    .itemName(donation.getItemDesc())
                    .quantity(donation.getQuantity())
                    .sourceType("DONATION")
                    .sourceId(donation.getId())
                    .operatorId(donation.getDonorId())
                    .operatorName(donation.getDonorName())
                    .remark("捐赠入库：" + donation.getDonationNo())
                    .build());
            return resp.isSuccess();
        } catch (Exception e) {
            log.error("调用物资服务入库异常：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public void cancel(String donationNo) {
        Donation donation = donationMapper.selectOne(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getDonationNo, donationNo));
        BizException.throwIfNull(donation, "捐赠单不存在");
        if (!"INIT".equals(donation.getStatus())) {
            throw new BizException("只有待支付的捐赠单可以取消");
        }
        Donation update = new Donation();
        update.setId(donation.getId());
        update.setStatus("CANCELED");
        donationMapper.updateById(update);
    }

    @Override
    public PageResult<Donation> page(long pageNum, long pageSize, String type, String status, String keyword) {
        Page<Donation> page = donationMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Donation>()
                        .eq(StrUtil.isNotBlank(type), Donation::getType, type)
                        .eq(StrUtil.isNotBlank(status), Donation::getStatus, status)
                        .and(StrUtil.isNotBlank(keyword), w -> w
                                .like(Donation::getDonationNo, keyword)
                                .or().like(Donation::getDonorName, keyword))
                        .orderByDesc(Donation::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public Donation detail(String donationNo) {
        Donation donation = donationMapper.selectOne(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getDonationNo, donationNo));
        BizException.throwIfNull(donation, "捐赠单不存在");
        return donation;
    }

    @Override
    public java.util.List<Donation> myDonations() {
        Long userId = UserContext.requireUserId();
        return donationMapper.selectList(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getDonorId, userId)
                .orderByDesc(Donation::getCreateTime));
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long total = donationMapper.selectCount(new LambdaQueryWrapper<Donation>());
        Long moneyCount = donationMapper.selectCount(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getType, "MONEY"));
        Long goodsCount = donationMapper.selectCount(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getType, "GOODS"));

        BigDecimal moneySum = donationMapper.selectList(new LambdaQueryWrapper<Donation>()
                        .eq(Donation::getType, "MONEY")
                        .in(Donation::getStatus, "PAID", "STORED"))
                .stream()
                .map(d -> d.getAmount() == null ? BigDecimal.ZERO : d.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.put("total", total);
        result.put("moneyCount", moneyCount);
        result.put("goodsCount", goodsCount);
        result.put("moneySum", moneySum);
        result.put("failCount", donationMapper.selectCount(new LambdaQueryWrapper<Donation>()
                .eq(Donation::getStatus, "FAILED")));
        return result;
    }

    private void sendNotify(Donation donation, String title, String content) {
        try {
            notifyClient.send(NotifyMessageDTO.builder()
                    .receiverId(donation.getDonorId() == null ? 1L : donation.getDonorId())
                    .receiverName(donation.getDonorName())
                    .title(title)
                    .content(content)
                    .type("DONATION")
                    .bizType("DONATION")
                    .bizId(donation.getId())
                    .channel("INNER")
                    .build());
        } catch (Exception e) {
            log.warn("捐赠通知发送失败：{}", e.getMessage());
        }
    }
}
