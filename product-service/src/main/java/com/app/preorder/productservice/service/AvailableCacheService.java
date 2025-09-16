package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String CACHE_KEY_PREFIX = "stock:avail:"; // AVAIL_PREFIX → 의미 드러나게

    // 하드 TTL(30분) + 지터(0~60초)
    private static final long HARD_TTL_SECONDS = 1800L;         // 30분
    private static final long HARD_TTL_JITTER_SECONDS = 60L;    // 0~60초 지터

    // SWR 재검증 임계치 — 밀리초 단위
    private static final long SOFT_TTL_MILLIS = 3_000L;         // 3초

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    /* 키 생성 */
    private String cacheKey(long productId) { return CACHE_KEY_PREFIX + productId; }

    // ─────────────────────────────────────────────────────────────────────────────
    // value|timestamp 인코딩/디코딩
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
    // ─────────────────────────────────────────────────────────────────────────────

    /** 단건 (read-through): 캐시 미스 시 DB → 캐시 set + TTL */
    public long get(long productId) {
        return getMany(List.of(productId)).getOrDefault(productId, 0L);
    }

    /** 다건 (read-through): mget → 미스만 DB → 배치 setex(+지터) */
    public Map<Long, Long> getMany(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();

        Map<Long, Long> result = new LinkedHashMap<>(productIds.size());
        List<String> keys = productIds.stream().map(this::cacheKey).toList();

        List<String> cachedValues = redis.getDataMulti(keys);
        List<Long> misses = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            String raw = (cachedValues != null && i < cachedValues.size()) ? cachedValues.get(i) : null;
            if (raw != null) {
                long[] decoded = decodeCacheValue(raw);
                if (decoded != null) {
                    result.put(productIds.get(i), Math.max(0L, decoded[0]));
                    continue;
                }
                try {
                    result.put(productIds.get(i), Math.max(0L, Long.parseLong(raw)));
                    continue;
                } catch (NumberFormatException ignore) { /* miss 처리 */ }
            }
            misses.add(productIds.get(i));
        }

        if (!misses.isEmpty()) {
            List<Long> uniqueMisses = misses.stream().distinct().toList();

            // 배치 1쿼리로 productId → available 조회
            Map<Long, Long> availableByProductId = stockRepository.findAvailableMapByProductIds(uniqueMisses);

            Map<String, String> toCache = new LinkedHashMap<>();
            long now = System.currentTimeMillis();
            for (Long productId : uniqueMisses) {
                long available = Math.max(0L, availableByProductId.getOrDefault(productId, 0L)); // 음수/NULL 하한 보정
                result.put(productId, available);
                toCache.put(cacheKey(productId), encodeCacheValue(available, now));
            }
            redis.setDataExpireBatch(toCache, HARD_TTL_SECONDS, HARD_TTL_JITTER_SECONDS); // ← 통일
        }

        return result;
    }

    /**
     * 커밋 이후: 캐시 무효화 → 비동기 재빌드(SWR)
     * - 트랜잭션 있으면 afterCommit, 없으면 즉시 실행
     */
    public void refreshAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        Runnable work = () -> {
            rebuildAsync(productIds);
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { work.run(); }
            });
        } else {
            work.run();
        }
    }

    // ───────────────────────────────
    // 핫패스 (cache-only + SWR)
    // ───────────────────────────────

    /** 단건: 키가 있으면 항상 숫자, 낡았으면 뒤에서 리빌드. 콜드면 0 반환 + 리빌드 */
    public long getFastSWR(long productId) {
        String raw = redis.getData(cacheKey(productId));
        long now = System.currentTimeMillis();
        long[] decoded = decodeCacheValue(raw);
        if (decoded != null) {
            long value = Math.max(0L, decoded[0]);
            long ts = decoded[1];
            if (ts > 0 && now - ts > SOFT_TTL_MILLIS) tryEnqueueRebuild(Set.of(productId)); // ← SOFT_TTL_MILLIS 사용
            return value;
        }
        // 콜드: 0 응답 + 백그라운드 채움
        tryEnqueueRebuild(Set.of(productId));
        return 0L;
    }

    /** 다건: 각 productId마다 숫자 응답. 낡았거나 콜드면 비동기 리빌드 */
    public Map<Long, Long> getManyFastSWR(List<Long> productIds) {
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
                if (ts > 0 && now - ts > SOFT_TTL_MILLIS) toRebuild.add(productId); // ← SOFT_TTL_MILLIS 사용
                out.put(productId, value);
            } else {
                toRebuild.add(productId);
                out.put(productId, 0L);
            }
        }

        if (!toRebuild.isEmpty()) tryEnqueueRebuild(toRebuild);
        return out;
    }

    // ───────────────────────────────
    // 백그라운드 리빌드(요청 경로 DB 0회 유지)
    // ───────────────────────────────

    private void tryEnqueueRebuild(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        rebuildAsync(new ArrayList<>(productIds)); // 최소버전: 락 없이 바로 큐잉
    }

    @Async("cacheUpdateExecutor")
    public void rebuildAsync(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        List<Long> unique = productIds.stream().distinct().toList();
        Map<Long, Long> availableByProductId = stockRepository.findAvailableMapByProductIds(unique);

        Map<String, String> toCache = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        for (Long productId : unique) {
            long available = Math.max(0L, availableByProductId.getOrDefault(productId, 0L));
            toCache.put(cacheKey(productId), encodeCacheValue(available, now));
        }
        redis.setDataExpireBatch(toCache, HARD_TTL_SECONDS, HARD_TTL_JITTER_SECONDS); // ← 통일
    }
}
