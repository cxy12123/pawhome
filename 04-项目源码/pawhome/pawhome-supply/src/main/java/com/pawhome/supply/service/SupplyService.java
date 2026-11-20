package com.pawhome.supply.service;

import com.pawhome.api.dto.SupplyInboundDTO;
import com.pawhome.common.core.PageResult;
import com.pawhome.supply.dto.StockChangeRequest;
import com.pawhome.supply.dto.SupplyItemVO;
import com.pawhome.supply.entity.SupplyRecord;

import java.util.List;
import java.util.Map;

public interface SupplyService {

    PageResult<SupplyItemVO> pageItems(long pageNum, long pageSize, String keyword,
                                       Long categoryId, Boolean onlyWarning);

    SupplyItemVO detail(Long id);

    Long createItem(SupplyItemVO vo);

    void updateItem(SupplyItemVO vo);

    /** 入库（带分布式锁） */
    void inbound(StockChangeRequest request);

    /** 出库（带分布式锁，库存不足抛异常） */
    void outbound(StockChangeRequest request);

    /** 供捐赠服务调用的幂等入库 */
    boolean inboundByDonation(SupplyInboundDTO dto);

    /** 查询库存 */
    Integer getStock(Long itemId);

    PageResult<SupplyRecord> pageRecords(Long itemId, long pageNum, long pageSize);

    List<SupplyItemVO> listWarning();

    Map<String, Object> statistics();
}
