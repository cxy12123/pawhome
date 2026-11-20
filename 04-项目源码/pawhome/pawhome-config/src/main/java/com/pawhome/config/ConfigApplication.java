package com.pawhome.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Spring Cloud Config 配置中心。
 * <p>
 * 对应教材 11.4.6「配置中心 Config」与 11.5.7「Spring Cloud Config 实现配置文件集中管理」。
 * 采用 native（本地文件系统）模式，配置文件放在 resources/config-repo 下。
 * <p>
 * 演示方式：
 * <pre>
 *   GET http://localhost:8888/pawhome-supply/default
 * </pre>
 * 返回该服务的集中配置内容。
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigApplication.class, args);
        System.out.println("""
                ====================================================
                  pawhome-config 启动成功
                  配置中心: http://localhost:8888/pawhome-supply/default
                ====================================================
                """);
    }
}
