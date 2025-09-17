package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String CACHE_KEY_PREFIX = "stock:avail:";
    // 소프트 TTL(밀리초) — 오래된 값이면 백그라운드 갱신 트리거
    private static final long CACHE_TTL_SOFT_MS = 3_000L;

    private final RedisUtil redis;
    private final AvailableCacheAsyncWorker worker;  // ✅ 워커 주입

    /* ===== READ: 캐시 우선(SWR) ===== */
    public long getCachedAvailable(long productId) {
        String raw = redis.getData(cacheKey(productId));
        long[] decoded = decode(raw);
        long now = System.currentTimeMillis();

        if (decoded != null) {
            long value = Math.max(0L, decoded[0]);
            long ts = decoded[1];
            if (ts > 0 && now - ts > CACHE_TTL_SOFT_MS) scheduleRebuild(Set.of(productId));
            return value;
        }
        scheduleRebuild(Set.of(productId)); // 콜드: 0 반환 + 비동기 채움
        return 0L;
    }

    public Map<Long, Long> getCachedAvailableBulk(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();
        List<String> keys = productIds.stream().map(this::cacheKey).toList();
        List<String> raws = redis.getDataMulti(keys);

        long now = System.currentTimeMillis();
        Map<Long, Long> out = new LinkedHashMap<>(productIds.size());
        List<Long> toRebuild = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            long pid = productIds.get(i);
            String raw = (raws != null && i < raws.size()) ? raws.get(i) : null;
            long[] decoded = decode(raw);
            if (decoded != null) {
                long v = Math.max(0L, decoded[0]);
                long ts = decoded[1];
                if (ts > 0 && now - ts > CACHE_TTL_SOFT_MS) toRebuild.add(pid);
                out.put(pid, v);
            } else {
                toRebuild.add(pid);
                out.put(pid, 0L);
            }
        }
        if (!toRebuild.isEmpty()) scheduleRebuild(toRebuild);
        return out;
    }

    /* ===== WRITE: 커밋 후 처리 ===== */
    public void refreshCacheAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { worker.rebuildCacheAsync(productIds); }
            });
        } else {
            worker.rebuildCacheAsync(productIds);
        }
    }

    public void evictCacheAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { worker.evictCacheAsync(productIds); }
            });
        } else {
            worker.evictCacheAsync(productIds);
        }
    }

    /* ===== 내부 유틸 ===== */
    private void scheduleRebuild(Collection<Long> ids) { worker.rebuildCacheAsync(ids); }
    private String cacheKey(long productId) { return CACHE_KEY_PREFIX + productId; }

    /** 저장 포맷: "value|timestampMillis" */
    private static long[] decode(String raw) {
        if (raw == null) return null;
        int sep = raw.indexOf('|');
        try {
            if (sep < 0) return new long[]{ Long.parseLong(raw), 0L };
            return new long[]{
                    Long.parseLong(raw.substring(0, sep)),
                    Long.parseLong(raw.substring(sep + 1))
            };
        } catch (Exception e) { return null; }
    }
}
