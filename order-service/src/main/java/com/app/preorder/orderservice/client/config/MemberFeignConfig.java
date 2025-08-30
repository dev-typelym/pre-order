package com.app.preorder.orderservice.client.config;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberFeignConfig {

    @Bean
    public feign.Request.Options feignOptions() {
        return new feign.Request.Options(800, TimeUnit.MILLISECONDS,
                1200, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public feign.Retryer retryer() {
        return feign.Retryer.NEVER_RETRY;
    }
}
