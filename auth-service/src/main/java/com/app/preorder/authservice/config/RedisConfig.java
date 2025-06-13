package com.app.preorder.authservice.config;

import com.app.preorder.infralib.util.RedisUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisUtil redisUtil(StringRedisTemplate stringRedisTemplate) {
        return new RedisUtil(stringRedisTemplate);
    }
}