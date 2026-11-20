package com.pawhome.api.config;

import com.pawhome.api.feign.fallback.AnimalClientFallbackFactory;
import com.pawhome.api.feign.fallback.NotifyClientFallbackFactory;
import com.pawhome.api.feign.fallback.SupplyClientFallbackFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 降级工厂自动装配。
 *
 * <p>各业务服务的启动类位于 com.pawhome.xxx 包下，默认的组件扫描范围
 * 覆盖不到 com.pawhome.api.feign.fallback。若在 @FeignClient 上声明了
 * fallbackFactory，SentinelFeign 创建代理时会按类型从容器里取实例，
 * 取不到就直接启动失败（No fallbackFactory instance ... found）。
 *
 * <p>这里通过 Spring Boot 3 的 AutoConfiguration.imports 统一注册，
 * 任何引入 pawhome-api 的服务都会自动获得这三个降级实例，
 * 无需在每个服务的启动类上额外加 @ComponentScan。
 */
@AutoConfiguration
public class FeignFallbackAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AnimalClientFallbackFactory.class)
    public AnimalClientFallbackFactory animalClientFallbackFactory() {
        return new AnimalClientFallbackFactory();
    }

    @Bean
    @ConditionalOnMissingBean(SupplyClientFallbackFactory.class)
    public SupplyClientFallbackFactory supplyClientFallbackFactory() {
        return new SupplyClientFallbackFactory();
    }

    @Bean
    @ConditionalOnMissingBean(NotifyClientFallbackFactory.class)
    public NotifyClientFallbackFactory notifyClientFallbackFactory() {
        return new NotifyClientFallbackFactory();
    }
}
