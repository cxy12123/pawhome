package com.pawhome.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 物资入库请求。
 * 由 donation 服务在捐赠支付成功后调用 supply 服务发起。
 * <p>
 * requestId 是幂等键：supply 服务收到后先查流水表有没有同 requestId 的记录，
 * 有就直接返回成功。这样即使网络超时导致重试，也不会重复入库。
 * 分布式系统里"至少一次投递 + 幂等消费"是保证最终一致的基本套路。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyInboundDTO implements Serializable {

    /** 幂等键，建议用捐赠单号 */
    private String requestId;
    /** 物资ID */
    private Long itemId;
    /** 物资名称（冗余，itemId 不存在时可按名称匹配） */
    private String itemName;
    /** 入库数量 */
    private Integer quantity;
    /** 来源类型：DONATION/PURCHASE/MANUAL */
    private String sourceType;
    /** 来源单据ID */
    private Long sourceId;
    /** 操作人ID */
    private Long operatorId;
    /** 操作人姓名 */
    private String operatorName;
    /** 备注 */
    private String remark;
    /** 参考单价 */
    private BigDecimal price;
}
