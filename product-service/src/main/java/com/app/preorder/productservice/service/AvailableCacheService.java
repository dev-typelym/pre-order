package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
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
        // 단건도 배치 경로로 통일 → 음수/NULL 보정, TTL/지터, 배치 DB 조회 로직 일관화
        return getMany(java.util.List.of(pid)).getOrDefault(pid, 0L);
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
                    // 캐시에 이미 있는 값은 그대로 사용(쓰기 시 하한 보정이 들어가므로 음수일 가능성 낮음)
                    result.put(pids.get(i), Long.parseLong(v));
                    continue;
                } catch (NumberFormatException ignore) { /* miss 처리 */ }
            }
            misses.add(pids.get(i));
        }

        if (!misses.isEmpty()) {
            List<Long> uniqueMisses = misses.stream().distinct().toList();

            // ✅ 엔티티 로드 대신 DB에서 바로 pid→available 맵 조회 (배치 1쿼리)
            Map<Long, Long> byPid = stockRepository.findAvailableMapByProductIds(uniqueMisses);

            Map<String, String> toCache = new LinkedHashMap<>();
            for (Long pid : uniqueMisses) {
                long available = Math.max(0L, byPid.getOrDefault(pid, 0L)); // 음수/NULL 하한 보정
                result.put(pid, available);
                toCache.put(key(pid), Long.toString(available));
            }
            // TTL + 지터 동일 적용
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
     * ✅ 커밋 이후: 캐시 무효화 → 즉시 재계산(웜업)
     * - 트랜잭션 있으면 afterCommit, 없으면 즉시 실행
     */
    public void refreshAfterCommit(Collection<Long> pids) {
        if (pids == null || pids.isEmpty()) return;

        Runnable work = () -> {
            invalidateMany(pids);
            // 재계산해서 캐시에 반영(반환값은 사용하지 않아도 캐시가 채워짐)
            getMany(new ArrayList<>(pids));
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
