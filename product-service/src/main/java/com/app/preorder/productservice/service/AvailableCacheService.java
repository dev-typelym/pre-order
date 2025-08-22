package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String AVAIL_PREFIX = "stock:avail:";
    private static final long TTL_SECONDS = 2L;
    private static final long TTL_JITTER_SECONDS = 1L; // 0~1초 지터

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    private String key(long pid) { return AVAIL_PREFIX + pid; }

    /** 단건 (캐시 미스 시 DB → 캐시 set + TTL+지터) */
    public long get(long pid) {
        String cached = redis.getData(key(pid));
        if (cached != null) return Long.parseLong(cached);

        long available = stockRepository.findAvailable(pid).orElse(0L);
        redis.setDataExpire(key(pid), String.valueOf(available), TTL_SECONDS, TTL_JITTER_SECONDS);
        return available;
    }

    /** 다건: mget → 미스만 DB → 배치 setex(+지터) */
    public Map<Long, Long> getMany(List<Long> pids) {
        if (pids == null || pids.isEmpty()) return Collections.emptyMap();

        Map<Long, Long> result = new LinkedHashMap<>(pids.size());
        List<String> keys = pids.stream().map(this::key).toList();

        List<String> values = redis.getDataMulti(keys);
        List<Long> misses = new ArrayList<>();

        for (int i = 0; i < pids.size(); i++) {
            String v = (values != null && i < values.size()) ? values.get(i) : null;
            if (v != null) {
                try {
                    result.put(pids.get(i), Long.parseLong(v));
                    continue;
                } catch (NumberFormatException ignore) { /* miss 처리 */ }
            }
            misses.add(pids.get(i));
        }

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

            Map<String, String> toCache = new LinkedHashMap<>();
            for (Long pid : uniqueMisses) {
                long available = byPid.getOrDefault(pid, 0L);
                result.put(pid, available);
                toCache.put(key(pid), Long.toString(available));
            }
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

    /**
     * ✅ 커밋 이후 캐시 무효화 + 재계산 실행, 결과를 콜백으로 전달
     * - 트랜잭션 있으면 afterCommit, 없으면 즉시 실행
     */
    public void refreshAfterCommit(Collection<Long> pids, BiConsumer<Long, Long> onAvailable) {
        if (pids == null || pids.isEmpty()) return;

        Runnable work = () -> {
            invalidateMany(pids);
            Map<Long, Long> av = getMany(new ArrayList<>(pids)); // DB→캐시 반영 후 계산
            for (Long pid : pids) {
                onAvailable.accept(pid, av.getOrDefault(pid, 0L));
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { work.run(); }
            });
        } else {
            work.run();
        }
    }
}
