package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailableCacheAsyncWorker {

    private static final String CACHE_KEY_PREFIX = "stock:avail:";
    private static final long CACHE_TTL_HARD_SECONDS = 1800L;
    private static final long CACHE_TTL_JITTER_SECONDS = 60L;

    private final StockRepository stockRepository;
    private final RedisUtil redis;

    @Async("cacheUpdateExecutor")
    public void rebuildCacheAsync(Collection<Long> productIds) {
        try {
            if (productIds == null || productIds.isEmpty()) return;
            List<Long> ids = productIds.stream().filter(Objects::nonNull).distinct().toList();

            Map<Long, Long> available = stockRepository.findAvailableMapByProductIds(ids);

            long now = System.currentTimeMillis();
            Map<String, String> batch = new LinkedHashMap<>(ids.size() * 2);
            for (Long pid : ids) {
                long v = Math.max(0L, available.getOrDefault(pid, 0L));
                batch.put(cacheKey(pid), encode(v, now));
            }
            redis.setDataExpireBatch(batch, CACHE_TTL_HARD_SECONDS, CACHE_TTL_JITTER_SECONDS);
        } catch (Exception e) {
            log.warn("Cache rebuild 실패. productIds={}", productIds, e);
        }
    }

    @Async("cacheUpdateExecutor")
    public void evictCacheAsync(Collection<Long> productIds) {
        try {
            if (productIds == null || productIds.isEmpty()) return;
            Set<String> keys = new HashSet<>();
            for (Long pid : productIds) keys.add(cacheKey(pid));
            redis.deleteDataBatch(keys);
        } catch (Exception e) {
            log.warn("Cache evict 실패. productIds={}", productIds, e);
        }
    }

    private static String cacheKey(long productId) { return CACHE_KEY_PREFIX + productId; }
    private static String encode(long value, long ts) { return value + "|" + ts; }
}
