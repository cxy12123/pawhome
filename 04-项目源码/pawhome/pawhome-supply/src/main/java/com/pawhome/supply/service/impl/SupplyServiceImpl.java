package com.pawhome.supply.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.common.constant.CommonConstants;
import com.pawhome.common.core.BizException;
import com.pawhome.common.core.PageResult;
import com.pawhome.supply.config.SupplyProperties;
import com.pawhome.supply.dto.StockChangeRequest;
import com.pawhome.supply.dto.SupplyItemVO;
import com.pawhome.supply.entity.SupplyItem;
import com.pawhome.supply.entity.SupplyRecord;
import com.pawhome.supply.mapper.SupplyItemMapper;
import com.pawhome.supply.mapper.SupplyRecordMapper;
import com.pawhome.supply.service.StockTxService;
import com.pawhome.supply.service.SupplyService;
import com.pawhome.common.util.BizNoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 物资服务实现。
 * <p>
 * 并发出库用 Redis 分布式锁串行化，这里说明为什么不用 synchronized：
 * <p>
 * synchronized 只在单个 JVM 内有效。服务部署 3 个实例时，
 * 三个 JVM 各有一把锁，等于没加锁，100 件库存会被扣成 -200。
 * 分布式锁把互斥范围扩展到"所有实例共享的 Redis"，才真正解决问题。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplyServiceImpl implements SupplyService {

    private final SupplyItemMapper itemMapper;
    private final SupplyRecordMapper recordMapper;
    private final StockTxService stockTxService;
    private final RedissonClient redissonClient;
    private final BizNoUtils bizNoUtils;
    private final SupplyProperties supplyProperties;

    @Override
    public PageResult<SupplyItemVO> pageItems(long pageNum, long pageSize, String keyword,
                                              Long categoryId, Boolean onlyWarning) {
        LambdaQueryWrapper<SupplyItem> wrapper = new LambdaQueryWrapper<SupplyItem>()
                .eq(categoryId != null, SupplyItem::getCategoryId, categoryId)
                .like(StrUtil.isNotBlank(keyword), SupplyItem::getName, keyword);
        // 只查告急的：直接用 SQL 比较两列，比查出来再过滤高效
        if (Boolean.TRUE.equals(onlyWarning)) {
            wrapper.apply("stock <= warn_stock");
        }
        wrapper.orderByAsc(SupplyItem::getStock);

        Page<SupplyItem> page = itemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<SupplyItemVO> records = page.getRecords().stream()
                .map(this::toVO).collect(Collectors.toList());
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplyItemVO detail(Long id) {
        SupplyItem item = itemMapper.selectById(id);
        BizException.throwIfNull(item, "物资不存在");
        return toVO(item);
    }

    @Override
    public Long createItem(SupplyItemVO vo) {
        BizException.throwIf(StrUtil.isBlank(vo.getName()), "物资名称不能为空");
        SupplyItem item = new SupplyItem();
        BeanUtil.copyProperties(vo, item, "id", "warning");
        item.setItemNo(bizNoUtils.supplyItemNo());
        item.setStock(vo.getStock() == null ? 0 : vo.getStock());
        item.setWarnStock(vo.getWarnStock() == null ? supplyProperties.getDefaultWarnStock() : vo.getWarnStock());
        item.setStatus(1);
        itemMapper.insert(item);
        return item.getId();
    }

    @Override
    public void updateItem(SupplyItemVO vo) {
        BizException.throwIf(vo.getId() == null, "修改时必须提供物资ID");
        BizException.throwIfNull(itemMapper.selectById(vo.getId()), "物资不存在");
        SupplyItem item = new SupplyItem();
        BeanUtil.copyProperties(vo, item, "warning");
        itemMapper.updateById(item);
    }

    @Override
    public void inbound(StockChangeRequest request) {
        checkQuantity(request.getQuantity());
        executeWithLock(request.getItemId(), () ->
                stockTxService.changeStock(request.getItemId(), request.getQuantity(), true,
                        request.getSourceType(), request.getSourceId(), request.getRemark()));
    }

    @Override
    public void outbound(StockChangeRequest request) {
        checkQuantity(request.getQuantity());
        executeWithLock(request.getItemId(), () ->
                stockTxService.changeStock(request.getItemId(), request.getQuantity(), false,
                        request.getSourceType(), request.getSourceId(), request.getRemark()));
    }

    /**
     * 捐赠入库（幂等）。
     * <p>
     * 幂等实现：以捐赠单号作为唯一凭据，如果流水表里已经存在
     * 这个来源单据的入库记录，说明之前已经成功处理过，直接返回成功。
     * <p>
     * 这样即使 Feign 超时重试、补偿任务重复执行，也不会重复入库。
     * 分布式系统中的"至少一次投递"必须配合"幂等消费"，否则数据必然出错。
     */
    @Override
    public boolean inboundByDonation(SupplyInboundDTO dto) {
        BizException.throwIf(dto.getItemId() == null, "缺少物资ID");
        BizException.throwIf(dto.getQuantity() == null || dto.getQuantity() <= 0, "入库数量必须大于 0");

        // 幂等检查：同一捐赠单只能入库一次
        if (dto.getSourceId() != null) {
            Long exists = recordMapper.selectCount(new LambdaQueryWrapper<SupplyRecord>()
                    .eq(SupplyRecord::getSourceType, "DONATION")
                    .eq(SupplyRecord::getSourceId, dto.getSourceId()));
            if (exists != null && exists > 0) {
                log.info("捐赠单 {} 已完成入库，本次请求按幂等直接返回成功", dto.getRequestId());
                return true;
            }
        }

        executeWithLock(dto.getItemId(), () ->
                stockTxService.changeStock(dto.getItemId(), dto.getQuantity(), true,
                        "DONATION", dto.getSourceId(),
                        StrUtil.isBlank(dto.getRemark()) ? "捐赠入库" : dto.getRemark(),
                        dto.getOperatorId(), dto.getOperatorName()));
        return true;
    }

    @Override
    public Integer getStock(Long itemId) {
        SupplyItem item = itemMapper.selectById(itemId);
        return item == null ? 0 : item.getStock();
    }

    @Override
    public PageResult<SupplyRecord> pageRecords(Long itemId, long pageNum, long pageSize) {
        Page<SupplyRecord> page = recordMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SupplyRecord>()
                        .eq(itemId != null, SupplyRecord::getItemId, itemId)
                        .orderByDesc(SupplyRecord::getCreateTime));
        return PageResult.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<SupplyItemVO> listWarning() {
        return itemMapper.selectList(new LambdaQueryWrapper<SupplyItem>()
                        .apply("stock <= warn_stock")
                        .orderByAsc(SupplyItem::getStock))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> statistics() {
        Map<String, Object> result = new LinkedHashMap<>();
        Long totalItems = itemMapper.selectCount(new LambdaQueryWrapper<SupplyItem>());
        Long warningCount = itemMapper.selectCount(new LambdaQueryWrapper<SupplyItem>()
                .apply("stock <= warn_stock"));
        result.put("totalItems", totalItems);
        result.put("warningCount", warningCount);
        result.put("inboundToday", recordMapper.selectCount(new LambdaQueryWrapper<SupplyRecord>()
                .eq(SupplyRecord::getType, "IN")
                .apply("DATE(create_time) = CURDATE()")));
        result.put("outboundToday", recordMapper.selectCount(new LambdaQueryWrapper<SupplyRecord>()
                .eq(SupplyRecord::getType, "OUT")
                .apply("DATE(create_time) = CURDATE()")));
        return result;
    }

    /**
     * 加锁执行。
     * <p>
     * 用 tryLock 而不是 lock()：等不到锁应该快速失败并提示用户重试，
     * 而不是把请求线程一直挂在那里。高并发下"快速失败"反而体验更好。
     * <p>
     * leaseSeconds 是锁的自动过期时间，防止持锁进程崩溃后锁永不释放。
     */
    private void executeWithLock(Long itemId, Runnable action) {
        String lockKey = CommonConstants.LOCK_SUPPLY_STOCK + itemId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(supplyProperties.getLockWaitSeconds(),
                    supplyProperties.getLockLeaseSeconds(), TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException("该物资正在被其他人操作，请稍后重试");
            }
            action.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("操作被中断，请重试");
        } finally {
            // isHeldByCurrentThread 防止误释放别人的锁
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void checkQuantity(Integer quantity) {
        BizException.throwIf(quantity == null || quantity <= 0, "数量必须大于 0");
        BizException.throwIf(quantity > supplyProperties.getMaxChangeQuantity(),
                "单次操作数量不能超过 " + supplyProperties.getMaxChangeQuantity());
    }

    private SupplyItemVO toVO(SupplyItem item) {
        SupplyItemVO vo = new SupplyItemVO();
        BeanUtil.copyProperties(item, vo);
        vo.setWarning(item.getStock() != null && item.getWarnStock() != null
                && item.getStock() <= item.getWarnStock());
        return vo;
    }
}
