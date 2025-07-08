package com.app.preorder.infralib.util;


import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;


public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String getData(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void setData(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public void setDataExpire(String key, String value, long durationInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(durationInSeconds));
    }

    public void deleteData(String key) {
        stringRedisTemplate.delete(key);
    }

    public Long incrementCount(String key, long expireSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(expireSeconds));
        }
        return count;
    }
}
