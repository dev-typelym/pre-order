package com.app.preorder.memberservice.config;

import com.app.preorder.infralib.util.RedisUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisUtil redisUtil(StringRedisTemplate template) {
        return new RedisUtil(template);
    }
}
