package com.pawhome.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 认证授权服务启动类。
 * <p>
 * 职责边界很清晰：只管"你是谁"（认证）和"你能干什么"（授权），
 * 不掺和任何业务数据。所以它的表里只有用户和登录日志，
 * 不出现动物、订单、库存这些字段。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.auth", "com.pawhome.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.pawhome.api.feign")
public class AuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-auth 启动成功
                  接口文档：http://localhost:8081/doc.html
                  默认账号：admin / 123456
                ====================================================
                """);
    }
}
