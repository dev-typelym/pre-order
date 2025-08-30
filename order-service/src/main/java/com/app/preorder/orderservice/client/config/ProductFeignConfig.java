package com.app.preorder.orderservice.client.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProductFeignConfig {

    @Bean
    public feign.Request.Options feignOptions() {
        // connectTimeout=800ms, readTimeout=1200ms, followRedirects=true
        return new feign.Request.Options(800, TimeUnit.MILLISECONDS,
                1200, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public feign.Retryer retryer() {
        // Feign 기본 재시도 비활성화 (Resilience4j로만 재시도 제어)
        return feign.Retryer.NEVER_RETRY;
    }
}