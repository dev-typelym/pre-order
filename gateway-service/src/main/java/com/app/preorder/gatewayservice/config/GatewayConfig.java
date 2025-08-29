package com.app.preorder.gatewayservice.config;

import java.net.InetSocketAddress;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();

            String uid = headers.getFirst("X-User-Id");
            if (uid != null && !uid.isBlank()) {
                return Mono.just("u:" + uid);
            }

            String xff = headers.getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String ipFromXff = xff.split(",")[0].trim();
                return Mono.just("ip:" + ipFromXff);
            }

            InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
            String ip = (remoteAddress != null && remoteAddress.getAddress() != null)
                    ? remoteAddress.getAddress().getHostAddress()
                    : "anon";
            return Mono.just("ip:" + ip);
        };
    }
}
