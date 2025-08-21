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
    private static final long TTL_JITTER_SECONDS = 1L; // ✅ 0~1초 지터 추가

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    private String key(long pid) { return AVAIL_PREFIX + pid; }

    /** 단건 (캐시 미스 시 DB → 캐시 set + TTL+지터) */
    public long get(long pid) {
        String cached = redis.getData(key(pid));
        if (cached != null) return Long.parseLong(cached);

        long available = stockRepository.findAvailable(pid).orElse(0L);
        // ✅ 지터 적용
        redis.setDataExpire(key(pid), String.valueOf(available), TTL_SECONDS, TTL_JITTER_SECONDS);
        return available;
    }

    /** 다건: mget → 미스만 DB → 배치 setex(+지터) */
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

            var stocks = stockRepository.findByProductIds(uniqueMisses);
            Map<Long, Long> byPid = stocks.stream().collect(Collectors.toMap(
                    s -> s.getProduct().getId(),
                    s -> Math.max(
                            (s.getStockQuantity() == null ? 0L : s.getStockQuantity())
                                    - (s.getReserved() == null ? 0L : s.getReserved()), 0L),
                    (a, b) -> a
            ));

            // 3) 결과 합치고 캐시 채우기(배치 setex + TTL+지터)
            Map<String, String> toCache = new LinkedHashMap<>();
            for (Long pid : uniqueMisses) {
                long available = byPid.getOrDefault(pid, 0L);
                result.put(pid, available);
                toCache.put(key(pid), Long.toString(available));
            }
            // ✅ 지터 적용
            redis.setDataExpireBatch(toCache, TTL_SECONDS, TTL_JITTER_SECONDS);
        }

        return result;
    }

    /** 변경 직후 무효화(단건) */
    public void invalidate(long pid) {
        redis.deleteData(key(pid));
    }

    /** 변경 직후 무효화(다건) */
    public void invalidateMany(Collection<Long> pids) {
        if (pids == null || pids.isEmpty()) return;
        Set<String> keys = pids.stream().map(this::key).collect(Collectors.toSet());
        redis.deleteDataBatch(keys);
    }
}
