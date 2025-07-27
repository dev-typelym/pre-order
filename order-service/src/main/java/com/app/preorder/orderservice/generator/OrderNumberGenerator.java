package com.app.preorder.orderservice.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OrderNumberGenerator {

    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "order:number:";
    private static final String ORDER_PREFIX = "ORD";

    public String generate() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);  // ex) 20250721
        String redisKey = REDIS_KEY_PREFIX + date;
        Long sequence = redisTemplate.opsForValue().increment(redisKey);

        if (sequence == 1L) {
            redisTemplate.expire(redisKey, Duration.ofDays(2));
        }

        return String.format("%s-%s-%06d", ORDER_PREFIX, date, sequence);
    }
}