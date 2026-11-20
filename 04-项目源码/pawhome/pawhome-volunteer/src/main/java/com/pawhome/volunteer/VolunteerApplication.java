package com.pawhome.volunteer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 志愿服务启动类。
 * <p>
 * 这个服务相对独立，只依赖通知服务（排班后提醒志愿者），
 * 是典型的"低耦合、可独立演进"的微服务：
 * 即使它挂掉，领养和捐赠主流程完全不受影响。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.volunteer", "com.pawhome.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pawhome.api.feign")
public class VolunteerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-volunteer 启动成功
                  接口文档：http://localhost:8086/doc.html
                ====================================================
                """);
    }
}
