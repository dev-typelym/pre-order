package com.app.preorder.authservice.util;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String getData(String key){
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void setData(String key, String value){
        stringRedisTemplate.opsForValue().set(key,value);
    }

    public void setDataExpire(String key, String value, long durationInSeconds){
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(durationInSeconds));
    }

    public void deleteData(String key){
        stringRedisTemplate.delete(key);
    }
}
