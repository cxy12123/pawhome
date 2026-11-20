package com.pawhome.supply.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 物资服务可动态调整的业务参数。
 * <p>
 * {@code @RefreshScope} 是关键注解：加上它之后，
 * 在 Nacos 控制台修改配置并发布，这个 Bean 会被销毁重建，
 * 新值立即生效，完全不需要重启服务。
 * <p>
 * 这就是配置中心相对"配置文件 + 重启"的核心价值：
 * 线上把预警阈值从 10 调到 20，只是改一个数字点发布，
 * 不影响正在处理的其他请求。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "pawhome.supply")
public class SupplyProperties {

    /** 新增物资时的默认预警阈值 */
    private int defaultWarnStock = 10;

    /** 库存低于阈值时是否发通知 */
    private boolean notifyOnWarn = true;

    /** 库存预警消息的接收人（默认发给内置管理员 admin） */
    private Long warnReceiverId = 1L;

    /** 单次出入库允许的最大数量，防止误操作一次扣掉全部库存 */
    private int maxChangeQuantity = 1000;

    /** 分布式锁等待时间（秒） */
    private long lockWaitSeconds = 3;

    /** 分布式锁持有时间（秒），超时自动释放防止死锁 */
    private long lockLeaseSeconds = 10;
}
