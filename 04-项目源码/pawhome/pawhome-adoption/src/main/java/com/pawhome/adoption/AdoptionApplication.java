package com.pawhome.adoption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 领养服务启动类。
 * <p>
 * 这个服务本身几乎不存业务主数据，它的价值在于"编排"：
 * 提交申请时向 animal 服务确认动物可领养，
 * 审核通过后驱动 animal 改状态、通知服务发消息。
 * <p>
 * 三个微服务的协作全部通过 Feign 完成，这就是微服务架构下
 * "业务能力按领域划分、通过接口协作"的具体体现。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.adoption", "com.pawhome.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pawhome.api.feign")
public class AdoptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdoptionApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-adoption 启动成功
                  接口文档：http://localhost:8083/doc.html
                ====================================================
                """);
    }
}
