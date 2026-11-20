package com.pawhome.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置。
 * <p>
 * 使用 Spring Cloud Gateway 内置的 RequestRateLimiter，底层是 Redis + Lua 的令牌桶算法：
 * - replenishRate：每秒补充多少令牌，即稳态 QPS
 * - burstCapacity：桶容量，允许的瞬时突发量
 * <p>
 * 这里定义两个 KeyResolver，实现差异化的限流维度：
 * - ipKeyResolver：按来源 IP 限流，用于登录接口，防止暴力撞库
 * - userKeyResolver：按用户限流，用于业务接口，防止单个用户刷接口
 * <p>
 * 为什么不用 Guava 的本地令牌桶？
 * 因为网关是多实例部署，本地桶各自计数会导致实际阈值被放大 N 倍，
 * 只有把桶放在 Redis 里集中计数才能保证全局限流。
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 按 IP 限流。
     * 注意：生产环境如果前面有 Nginx，要取 X-Forwarded-For 的第一个地址，
     * 否则拿到的全是 Nginx 的内网 IP，等于所有用户共用一个桶。
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress() == null
                    ? "unknown"
                    : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just("rate:ip:" + ip);
        };
    }

    /** 按用户限流，优先用网关解析出的用户ID，取不到退化为 IP */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null || userId.isBlank()) {
                String ip = exchange.getRequest().getRemoteAddress() == null
                        ? "unknown"
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                return Mono.just("rate:ip:" + ip);
            }
            return Mono.just("rate:user:" + userId);
        };
    }
}
