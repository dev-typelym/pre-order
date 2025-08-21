package com.app.preorder.infralib.util;

import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SessionCallback;           // ✅ 추가
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;                                         // ✅ 추가
import java.util.List;                                              // ✅ 추가
import java.util.Map;                                               // ✅ 추가
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtil(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 단건 값 조회
    public String getData(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // 단건 값 설정(만료시간 없음)
    public void setData(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    // 단건 값 설정 + 만료시간(초) - 기본(지터 없음, 현재 코드 유지)
    public void setDataExpire(String key, String value, long durationInSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(durationInSeconds));
    }

    // ✅ 단건 값 설정 + 만료시간(초) + 지터(초)
    public void setDataExpire(String key, String value, long baseSeconds, long jitterMaxSeconds) {
        long ttl = baseSeconds + (jitterMaxSeconds > 0 ? ThreadLocalRandom.current().nextLong(jitterMaxSeconds + 1) : 0);
        stringRedisTemplate.opsForValue().set(key, value);
        stringRedisTemplate.expire(key, ttl, TimeUnit.SECONDS);
    }

    // 여러 키의 값을 한 번에 조회(mget) — 순서=입력 keys, 없으면 null
    public List<String> getDataMulti(List<String> keys) {
        return stringRedisTemplate.opsForValue().multiGet(keys);
    }

    // 여러 키를 배치로 SET + EXPIRE(초) 처리(파이프라이닝) - 기본(지터 없음)
    public void setDataExpireBatch(Map<String, String> kv, long durationInSeconds) {
        setDataExpireBatch(kv, durationInSeconds, 0);
    }

    // ✅ 여러 키 배치 SETEX + 지터(초) (파이프라이닝)
    public void setDataExpireBatch(Map<String, String> kv, long baseSeconds, long jitterMaxSeconds) {
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection c = (StringRedisConnection) connection;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            kv.forEach((k, v) -> {
                long ttl = baseSeconds + (jitterMaxSeconds > 0 ? rnd.nextLong(jitterMaxSeconds + 1) : 0);
                c.setEx(k, ttl, v); // 단일 명령으로 TTL 포함(SETEX)
            });
            return null;
        });
    }

    // 단건 키 삭제
    public void deleteData(String key) {
        stringRedisTemplate.delete(key);
    }

    // 여러 키 삭제(del 다건)
    public void deleteDataBatch(Collection<String> keys) {
        stringRedisTemplate.delete(keys);
    }

    // 카운터 증가(처음 증가 시 TTL 세팅)
    public Long incrementCount(String key, long expireSeconds) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(expireSeconds));
        }
        return count;
    }

    // Set에 값 추가
    public void addSet(String key, String value) {
        stringRedisTemplate.opsForSet().add(key, value);
    }

    // Set 멤버 조회
    public Set<String> getSetMembers(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    // Set에서 특정 멤버 제거
    public void removeSetMember(String key, String value) {
        stringRedisTemplate.opsForSet().remove(key, value);
    }
}
