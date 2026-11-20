package com.pawhome.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 通知服务启动类。
 * <p>
 * 这是全平台唯一负责"发消息"的服务。
 * 领养审核、库存预警、捐赠致谢都由它统一处理，
 * 好处是将来要接入短信、邮件、企业微信，只改这一个服务，
 * 其他业务服务一行代码都不用动 —— 这就是"关注点分离"的价值。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.notify", "com.pawhome.common"})
@EnableDiscoveryClient
public class NotifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-notify 启动成功
                  接口文档：http://localhost:8087/doc.html
                ====================================================
                """);
    }
}
