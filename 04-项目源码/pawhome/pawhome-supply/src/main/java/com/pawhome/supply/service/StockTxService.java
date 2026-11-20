package com.pawhome.supply.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pawhome.api.dto.NotifyMessageDTO;
import com.pawhome.api.feign.NotifyClient;
import com.pawhome.common.context.UserContext;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.ResultCode;
import com.pawhome.common.util.BizNoUtils;
import com.pawhome.supply.config.SupplyProperties;
import com.pawhome.supply.dto.StockChangeRequest;
import com.pawhome.supply.entity.SupplyItem;
import com.pawhome.supply.entity.SupplyRecord;
import com.pawhome.supply.mapper.SupplyItemMapper;
import com.pawhome.supply.mapper.SupplyRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存变更的事务单元。
 * <p>
 * 为什么单独抽一个类？
 * 因为分布式锁必须包在事务外层 —— 如果写成
 * <pre>
 *   &#64;Transactional
 *   public void outbound() {
 *       lock.lock();
 *       ...改库存...
 *       lock.unlock();   // 这里解锁时事务还没提交！
 *   }
 * </pre>
 * 那么解锁后别的事务可能读到未提交的数据，锁白白加了。
 * 正确顺序是：加锁 -> 开启事务 -> 提交事务 -> 解锁。
 * <p>
 * 把事务逻辑放到独立 Bean，由外层的 SupplyServiceImpl 加锁后调用，
 * 才能保证 Spring 的事务代理真正生效（同一个类内部调用不走代理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockTxService {

    private final SupplyItemMapper itemMapper;
    private final SupplyRecordMapper recordMapper;
    private final BizNoUtils bizNoUtils;
    private final SupplyProperties supplyProperties;
    private final NotifyClient notifyClient;

    @Transactional(rollbackFor = Exception.class)
    public void changeStock(Long itemId, int quantity, boolean inbound,
                            String sourceType, Long sourceId, String remark) {
        changeStock(itemId, quantity, inbound, sourceType, sourceId, remark, null, null);
    }

    /**
     * 带操作人的版本。
     * <p>
     * 服务间调用（捐赠入库）没有登录上下文，UserContext 是空的，
     * 由调用方把捐赠人信息传进来，流水里才看得出这笔记在谁头上。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStock(Long itemId, int quantity, boolean inbound,
                            String sourceType, Long sourceId, String remark,
                            Long operatorId, String operatorName) {
        SupplyItem item = itemMapper.selectById(itemId);
        BizException.throwIfNull(item, "物资不存在");

        int beforeStock = item.getStock() == null ? 0 : item.getStock();
        int afterStock;

        if (inbound) {
            int affected = itemMapper.addStock(itemId, quantity);
            BizException.throwIf(affected == 0, "入库失败，物资可能已被删除");
            afterStock = beforeStock + quantity;
        } else {
            // CAS 扣减：库存不足时 affected = 0
            int affected = itemMapper.deductStock(itemId, quantity);
            if (affected == 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                        "「" + item.getName() + "」库存不足，当前仅剩 " + beforeStock + " " + item.getUnit());
            }
            afterStock = beforeStock - quantity;
        }

        // 写流水：只增不改，作为对账依据
        SupplyRecord record = new SupplyRecord();
        record.setRecordNo(bizNoUtils.supplyRecordNo());
        record.setItemId(itemId);
        record.setItemName(item.getName());
        record.setType(inbound ? "IN" : "OUT");
        record.setQuantity(quantity);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setSourceType(sourceType == null ? "MANUAL" : sourceType);
        record.setSourceId(sourceId);
        // 操作人：优先用调用方传入的（服务间调用），否则取当前登录用户
        String finalOperatorName = (operatorName == null || operatorName.isBlank())
                ? (UserContext.getRealName() != null ? UserContext.getRealName() : UserContext.getUsername())
                : operatorName;
        record.setOperatorId(operatorId != null ? operatorId : UserContext.getUserId());
        record.setOperatorName(finalOperatorName);
        record.setRemark(remark);
        recordMapper.insert(record);

        log.info("库存变更：{} {} {} {} -> {}", item.getName(),
                inbound ? "入库" : "出库", quantity, beforeStock, afterStock);

        // 低于预警线时提醒：日志 + 站内信给管理员
        // 只在"刚刚跌到线下"时发一次（出库后不足，或入库后仍然不足），避免每次操作都刷屏
        boolean justBelow = afterStock <= item.getWarnStock()
                && (beforeStock > item.getWarnStock() || !inbound);
        if (supplyProperties.isNotifyOnWarn() && justBelow) {
            log.warn("【库存预警】物资「{}」剩余 {} {}，已低于预警值 {}",
                    item.getName(), afterStock, item.getUnit(), item.getWarnStock());
            notifyWarn(item, afterStock);
        }
    }

    /**
     * 发库存预警站内信。
     * <p>
     * 通知是旁路：notify 服务挂了也不能让出入库失败，
     * 所以单独 try-catch，异常只记日志。
     */
    private void notifyWarn(SupplyItem item, int afterStock) {
        try {
            notifyClient.send(NotifyMessageDTO.builder()
                    .receiverId(supplyProperties.getWarnReceiverId())
                    .receiverName("系统管理员")
                    .title("物资库存不足提醒")
                    .content(String.format("「%s」当前剩余 %d %s，已低于预警值 %d，请及时补货。",
                            item.getName(), afterStock, item.getUnit(), item.getWarnStock()))
                    .type("SUPPLY")
                    .bizType("SUPPLY_ITEM")
                    .bizId(item.getId())
                    .channel("INNER")
                    .build());
        } catch (Exception e) {
            log.warn("库存预警通知发送失败，不影响库存变更：{}", e.getMessage());
        }
    }
}
