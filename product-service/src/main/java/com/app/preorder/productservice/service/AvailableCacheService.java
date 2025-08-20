package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String AVAIL_PREFIX = "stock:avail:";
    private static final long TTL_SECONDS = 2L;

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    private String key(long pid) { return AVAIL_PREFIX + pid; }

    /** 단건 (그대로) */
    public long get(long pid) {
        String cached = redis.getData(key(pid));
        if (cached != null) return Long.parseLong(cached);

        long available = stockRepository.findAvailable(pid).orElse(0L);
        redis.setDataExpire(key(pid), String.valueOf(available), TTL_SECONDS);
        return available;
    }

    /** 다건: mget → 미스만 DB → 파이프라인 setex */
    public Map<Long, Long> getMany(List<Long> pids) {
        if (pids == null || pids.isEmpty()) return Collections.emptyMap();

        Map<Long, Long> result = new LinkedHashMap<>(pids.size());
        List<String> keys = pids.stream().map(this::key).toList();

        // 1) 캐시 다건 조회(mget)
        List<String> values = redis.getDataMulti(keys); // 값 없으면 각 슬롯에 null
        List<Long> misses = new ArrayList<>();

        for (int i = 0; i < pids.size(); i++) {
            String v = (values != null && i < values.size()) ? values.get(i) : null;
            if (v != null) {
                try {
                    result.put(pids.get(i), Long.parseLong(v));
                    continue;
                } catch (NumberFormatException ignore) { /* fallthrough to miss */ }
            }
            misses.add(pids.get(i));
        }

        // 2) 캐시 미스만 DB에서 조회(중복 제거)
        if (!misses.isEmpty()) {
            List<Long> uniqueMisses = misses.stream().distinct().toList();

            // (A) Java에서 계산: findByProductIds가 있는 경우
            var stocks = stockRepository.findByProductIds(uniqueMisses);
            Map<Long, Long> byPid = stocks.stream().collect(Collectors.toMap(
                    s -> s.getProduct().getId(),
                    s -> Math.max(
                            (s.getStockQuantity() == null ? 0L : s.getStockQuantity())
                                    - (s.getReserved() == null ? 0L : s.getReserved()), 0L),
                    (a, b) -> a   // 중복 키 병합 전략
            ));

            // (B) 만약 DB에서 바로 available을 주는 쿼리가 있다면:
            // Map<Long, Long> byPid = stockRepository.findAvailableMany(uniqueMisses);

            // 3) 결과 합치고 캐시 채우기(배치 set + TTL)
            Map<String, String> toCache = new LinkedHashMap<>();
            for (Long pid : uniqueMisses) {
                long available = byPid.getOrDefault(pid, 0L);
                result.put(pid, available);
                toCache.put(key(pid), Long.toString(available));
            }
            redis.setDataExpireBatch(toCache, TTL_SECONDS);
        }

        return result;
    }

    /** 변경 직후 무효화 */
    public void invalidate(long pid) {
        redis.deleteData(key(pid));
    }

    public void invalidateMany(Collection<Long> pids) {
        if (pids == null || pids.isEmpty()) return;
        Set<String> keys = pids.stream().map(this::key).collect(Collectors.toSet());
        redis.deleteDataBatch(keys);
    }
}
