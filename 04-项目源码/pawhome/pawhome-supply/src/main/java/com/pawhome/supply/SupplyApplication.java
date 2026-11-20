package com.pawhome.supply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 物资库存服务启动类。
 * <p>
 * 这个服务承载了两个高价值技术点：
 * 1. Redisson 分布式锁 —— 多实例并发扣减库存不会超卖；
 * 2. Nacos 配置中心 —— 库存预警阈值可以在线调整，无需重启。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.supply", "com.pawhome.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pawhome.api.feign")
public class SupplyApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupplyApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-supply 启动成功
                  接口文档：http://localhost:8084/doc.html
                ====================================================
                """);
    }
}
