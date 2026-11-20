package com.pawhome.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * PawHome 统一网关启动类。
 * <p>
 * 整个平台只有这一个入口，前端所有请求都发到 8080，
 * 由网关根据路径前缀转发到对应的微服务实例。
 * <p>
 * 网关承担五件事：
 * 1. 路由转发（lb:// 走 Nacos + LoadBalancer 负载均衡）
 * 2. 统一鉴权（JWT 验签，解析出用户身份后透传给下游）
 * 3. 限流（Redis 令牌桶，保护登录接口不被暴力破解）
 * 4. 跨域（前端与接口同源后其实用不上，但保留便于本地调试）
 * 5. 托管前端静态页面（免去单独部署 Nginx）
 */
@SpringBootApplication
@EnableDiscoveryClient
public class PawHomeGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PawHomeGatewayApplication.class, args);
        System.out.println("""
                ====================================================
                  PawHome 网关启动成功
                  管理后台：http://localhost:8080/
                  健康检查：http://localhost:8080/actuator/health
                ====================================================
                """);
    }
}
