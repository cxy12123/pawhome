package com.pawhome.donation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 捐赠服务启动类。
 * <p>
 * 开启定时任务是为了运行"补偿任务"：
 * 捐赠支付成功后需要调用 supply 服务入库，如果那一刻 supply 挂了，
 * 补偿表会记录一条待处理任务，由定时任务在后续重试直到成功。
 * <p>
 * 这是不引入 Seata 时实现最终一致性的标准做法，
 * 也叫"本地消息表"模式的简化版。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.donation", "com.pawhome.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pawhome.api.feign")
@EnableScheduling
public class DonationApplication {

    public static void main(String[] args) {
        SpringApplication.run(DonationApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-donation 启动成功
                  接口文档：http://localhost:8085/doc.html
                  补偿任务：每 30 秒扫描一次待处理记录
                ====================================================
                """);
    }
}
