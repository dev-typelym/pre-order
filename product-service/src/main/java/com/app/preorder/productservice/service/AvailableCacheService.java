package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String CACHE_KEY_PREFIX = "stock:avail:";

    // 하드 TTL(30분) + 지터(0~60초)
    private static final long CACHE_TTL_HARD_SECONDS = 1800L;
    private static final long CACHE_TTL_JITTER_SECONDS = 60L;

    // SWR 소프트 TTL(밀리초)
    private static final long CACHE_TTL_SOFT_MS = 3_000L;

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    /* ─────────────────────────────
     * 1) READ — 핫패스(SWR, DB 미접근)
     * ───────────────────────────── */
    /** 캐시에서 즉시 읽기(스테일/미스면 비동기 리빌드 트리거) */
    public long getCachedAvailable(long productId) {
        String raw = redis.getData(cacheKey(productId));
        long now = System.currentTimeMillis();
        long[] decoded = decodeCacheValue(raw); // ← encode/decode는 Value 명칭 유지
        if (decoded != null) {
            long value = Math.max(0L, decoded[0]);
            long ts = decoded[1];
            if (ts > 0 && now - ts > CACHE_TTL_SOFT_MS) scheduleRebuild(Set.of(productId));
            return value;
        }
        // 콜드: 0 응답 + 백그라운드 채움
        scheduleRebuild(Set.of(productId));
        return 0L;
    }

    /** 다건 버전 */
    public Map<Long, Long> getCachedAvailableBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();

        List<String> keys = productIds.stream().map(this::cacheKey).toList();
        List<String> raws = redis.getDataMulti(keys);

        long now = System.currentTimeMillis();
        Map<Long, Long> out = new LinkedHashMap<>(productIds.size());
        List<Long> toRebuild = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            long productId = productIds.get(i);
            String raw = (raws != null && i < raws.size()) ? raws.get(i) : null;
            long[] decoded = decodeCacheValue(raw);
            if (decoded != null) {
                long value = Math.max(0L, decoded[0]);
                long ts = decoded[1];
                if (ts > 0 && now - ts > CACHE_TTL_SOFT_MS) toRebuild.add(productId);
                out.put(productId, value);
            } else {
                toRebuild.add(productId);
                out.put(productId, 0L);
            }
        }

        if (!toRebuild.isEmpty()) scheduleRebuild(toRebuild);
        return out;
    }

    /* ─────────────────────────────
     * 2) WRITE — 커밋 이후 처리(리빌드/삭제)
     * ───────────────────────────── */
    /** 커밋 이후: 캐시 최신값으로 재빌드 */
    public void refreshCacheAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        Runnable work = () -> rebuildCacheAsync(productIds);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { work.run(); }
            });
        } else {
            work.run();
        }
    }

    /** 커밋 이후: 캐시 키 제거(상품 삭제 등) */
    public void evictCacheAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        Runnable work = () -> {
            Set<String> keys = productIds.stream().map(this::cacheKey).collect(Collectors.toSet());
            redis.deleteDataBatch(keys);
        };
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { work.run(); }
            });
        } else {
            work.run();
        }
    }

    /* ─────────────────────────────
     * 3) 백그라운드 리빌드
     * ───────────────────────────── */
    private void scheduleRebuild(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        rebuildCacheAsync(new ArrayList<>(productIds)); // 최소 버전: 락 없이 즉시 @Async 실행
    }

    @Async("cacheUpdateExecutor")
    public void rebuildCacheAsync(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        List<Long> unique = productIds.stream().distinct().toList();
        Map<Long, Long> availableByProductId = stockRepository.findAvailableMapByProductIds(unique);

        Map<String, String> toCache = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        for (Long productId : unique) {
            long available = Math.max(0L, availableByProductId.getOrDefault(productId, 0L));
            toCache.put(cacheKey(productId), encodeCacheValue(available, now));
        }
        redis.setDataExpireBatch(toCache, CACHE_TTL_HARD_SECONDS, CACHE_TTL_JITTER_SECONDS);
    }

    /* ─────────────────────────────
     * 4) 유틸
     * ───────────────────────────── */
    private String cacheKey(long productId) { return CACHE_KEY_PREFIX + productId; }

    // ★ 요청대로 encode/decode는 Value 명칭 유지
    private String encodeCacheValue(long value, long timestampMillis) {
        return value + "|" + timestampMillis;
    }

    /** @return [value, timestampMillis] or null */
    private long[] decodeCacheValue(String raw) {
        if (raw == null) return null;
        int sep = raw.indexOf('|');
        try {
            if (sep < 0) return new long[]{ Long.parseLong(raw), 0L }; // 구버전(숫자-only) 호환
            return new long[]{
                    Long.parseLong(raw.substring(0, sep)),
                    Long.parseLong(raw.substring(sep + 1))
            };
        } catch (Exception e) {
            return null;
        }
    }
}
