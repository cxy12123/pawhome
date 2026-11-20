package com.pawhome.supply.service;

import com.pawhome.api.feign.NotifyClient;
import com.pawhome.common.core.BizException;
import com.pawhome.common.util.BizNoUtils;
import com.pawhome.supply.config.SupplyProperties;
import com.pawhome.supply.entity.SupplyItem;
import com.pawhome.supply.entity.SupplyRecord;
import com.pawhome.supply.mapper.SupplyItemMapper;
import com.pawhome.supply.mapper.SupplyRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存变更单元测试。
 * <p>
 * 重点验证三件事：
 * 1. 库存不足时必须抛业务异常，且不能留下流水；
 * 2. 出库后跌到预警线要发站内信；
 * 3. 服务间调用（捐赠入库）能带上捐赠人作为操作人。
 */
@ExtendWith(MockitoExtension.class)
class StockTxServiceTest {

    @Mock
    private SupplyItemMapper itemMapper;
    @Mock
    private SupplyRecordMapper recordMapper;
    @Mock
    private BizNoUtils bizNoUtils;
    @Mock
    private NotifyClient notifyClient;

    private SupplyProperties properties;
    private StockTxService service;

    @BeforeEach
    void setUp() {
        properties = new SupplyProperties();
        service = new StockTxService(itemMapper, recordMapper, bizNoUtils, properties, notifyClient);
    }

    private SupplyItem item(int stock, int warnStock) {
        SupplyItem item = new SupplyItem();
        item.setId(4001L);
        item.setName("成犬狗粮");
        item.setUnit("袋");
        item.setStock(stock);
        item.setWarnStock(warnStock);
        return item;
    }

    @Test
    @DisplayName("库存不足：抛业务异常且不写流水")
    void outboundNotEnough() {
        when(itemMapper.selectById(4001L)).thenReturn(item(3, 5));
        when(itemMapper.deductStock(4001L, 5)).thenReturn(0);

        assertThrows(BizException.class,
                () -> service.changeStock(4001L, 5, false, "ISSUE", null, "领用"));

        verify(recordMapper, never()).insert(any());
        verify(notifyClient, never()).send(any());
    }

    @Test
    @DisplayName("出库后低于预警值：写流水并给管理员发预警通知")
    void outboundTriggersWarnNotify() {
        when(itemMapper.selectById(4001L)).thenReturn(item(6, 5));
        when(itemMapper.deductStock(4001L, 2)).thenReturn(1);
        when(bizNoUtils.supplyRecordNo()).thenReturn("SR202601010001");

        service.changeStock(4001L, 2, false, "ISSUE", null, "领用");

        ArgumentCaptor<SupplyRecord> captor = ArgumentCaptor.forClass(SupplyRecord.class);
        verify(recordMapper).insert(captor.capture());
        SupplyRecord record = captor.getValue();
        assertEquals("OUT", record.getType());
        assertEquals(6, record.getBeforeStock());
        assertEquals(4, record.getAfterStock());
        assertEquals(2, record.getQuantity());

        verify(notifyClient).send(any());
    }

    @Test
    @DisplayName("入库后库存充足：不发预警")
    void inboundNoWarn() {
        when(itemMapper.selectById(4001L)).thenReturn(item(20, 5));
        when(itemMapper.addStock(4001L, 3)).thenReturn(1);
        when(bizNoUtils.supplyRecordNo()).thenReturn("SR202601010002");

        service.changeStock(4001L, 3, true, "PURCHASE", null, "采购");

        verify(recordMapper).insert(any());
        verify(notifyClient, never()).send(any());
    }

    @Test
    @DisplayName("服务间调用可指定操作人（捐赠入库记在捐赠人名下）")
    void operatorFromCaller() {
        when(itemMapper.selectById(4001L)).thenReturn(item(20, 5));
        when(itemMapper.addStock(4001L, 1)).thenReturn(1);
        when(bizNoUtils.supplyRecordNo()).thenReturn("SR202601010003");

        service.changeStock(4001L, 1, true, "DONATION", 9L, "捐赠入库", 4L, "张领养");

        ArgumentCaptor<SupplyRecord> captor = ArgumentCaptor.forClass(SupplyRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals("张领养", captor.getValue().getOperatorName());
        assertEquals(Long.valueOf(4L), captor.getValue().getOperatorId());
        assertEquals("DONATION", captor.getValue().getSourceType());
    }
}
