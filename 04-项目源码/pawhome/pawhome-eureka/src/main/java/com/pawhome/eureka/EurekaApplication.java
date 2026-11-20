package com.pawhome.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka 服务注册中心。
 * <p>
 * 对应教材 11.4.2「服务发现框架 Eureka」与 11.5.2「搭建 Eureka 服务中心」。
 * 访问 http://localhost:8761 可查看已注册的服务实例。
 * <p>
 * 说明：本项目的主注册中心是 Nacos，8 个业务服务同时注册到 Eureka，
 * 用于演示教材中的 Eureka 面板；服务发现与路由仍由 Nacos 承担，
 * 因此不会影响既有链路。
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-eureka 启动成功
                  Eureka Dashboard: http://localhost:8761
                ====================================================
                """);
    }
}
