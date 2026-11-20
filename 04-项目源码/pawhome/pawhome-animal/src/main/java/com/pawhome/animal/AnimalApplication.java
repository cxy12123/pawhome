package com.pawhome.animal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 动物档案服务启动类。
 * <p>
 * 这是整个平台的"主数据"服务：动物的存在、健康、可领养状态都在这里。
 * 领养服务、捐赠服务都通过 Feign 回调它，所以它的可用性要求最高，
 * 实验中对它配置了 Sentinel 熔断规则作为保护。
 */
@SpringBootApplication(scanBasePackages = {"com.pawhome.animal", "com.pawhome.common"})
@EnableDiscoveryClient
public class AnimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimalApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-animal 启动成功
                  接口文档：http://localhost:8082/doc.html
                ====================================================
                """);
    }
}
