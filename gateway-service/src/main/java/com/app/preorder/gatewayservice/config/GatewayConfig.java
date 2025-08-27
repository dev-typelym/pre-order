package com.app.preorder.gatewayservice.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String key = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (key == null && exchange.getRequest().getRemoteAddress() != null) {
                key = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            return Mono.just(key != null ? key : "anon");
        };
    }
}
