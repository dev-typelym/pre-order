package com.app.preorder.productservice.service;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

/**
 * 가용 재고 캐시 서비스
 * - Redis MGET → 미스만 DB 일괄 조회 → SETEX(지터)로 덮어쓰기
 * - 커밋 이후엔 무효화 대신 "DB→SETEX 덮어쓰기"로 바로 웜업
 * - 삭제/숨김은 afterCommit에서 EVICT(DEL)
 * - 대용량 배치 대비 안전 청크(500)
 */
@Service
@RequiredArgsConstructor
public class AvailableCacheService {

    private static final String AVAIL_PREFIX = "stock:avail:";
    // 현실적인 TTL/지터: 너무 짧지 않게, 스탬피드 완화
    private static final long TTL_SECONDS = 1800L;       // 30분
    private static final long TTL_JITTER_SECONDS = 300L; // 5분

    // 대용량 pid 배치 안정 가드(평소엔 영향 미미)
    private static final int CHUNK_SIZE = 500;

    private final RedisUtil redis;
    private final StockRepository stockRepository;

    private String cacheKey(long pid) { return AVAIL_PREFIX + pid; }

    /** 단건 조회: 내부적으로 배치 경로 사용 */
    public long getAvailable(long productId) {
        return getAvailableMap(Collections.singletonList(productId))
                .getOrDefault(productId, 0L);
    }

    /**
     * 다건 조회:
     * 1) Redis MGET
     * 2) 미스만 모아(중복 제거) DB 일괄 조회 (청크 분할)
     * 3) SETEX(+지터)로 캐시 덮어쓰기 → 웜업
     */
    public Map<Long, Long> getAvailableMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return Collections.emptyMap();

        // 입력 보존 순서 유지 + 중복 제거
        List<Long> uniqueIds = new ArrayList<>(new LinkedHashSet<>(productIds));
        Map<Long, Long> result = new LinkedHashMap<>(uniqueIds.size());

        // 1) 캐시 일괄 조회
        List<String> keys = uniqueIds.stream().map(this::cacheKey).toList();
        List<String> cachedValues = redis.getDataMulti(keys);

        List<Long> misses = new ArrayList<>();
        for (int i = 0; i < uniqueIds.size(); i++) {
            Long pid = uniqueIds.get(i);
            String raw = (cachedValues != null && i < cachedValues.size()) ? cachedValues.get(i) : null;
            if (raw != null) {
                try {
                    result.put(pid, Long.parseLong(raw));
                    continue;
                } catch (NumberFormatException ignore) {
                    // 캐시 값이 손상된 경우 미스로 처리
                }
            }
            misses.add(pid);
        }

        // 2) 미스만 DB 조회(+청크)
        if (!misses.isEmpty()) {
            for (int i = 0; i < misses.size(); i += CHUNK_SIZE) {
                List<Long> sub = misses.subList(i, Math.min(i + CHUNK_SIZE, misses.size()));

                // 엔티티 로드 금지, 바로 pid->available 맵 조회
                Map<Long, Long> fromDb = stockRepository.findAvailableMapByProductIds(sub);

                // 3) 캐시 덮어쓰기(SETEX+지터) 및 결과 합치기
                Map<String, String> toCache = new LinkedHashMap<>(sub.size());
                for (Long pid : sub) {
                    long available = Math.max(0L, fromDb.getOrDefault(pid, 0L)); // 음수/NULL 하한 보정
                    result.put(pid, available);
                    toCache.put(cacheKey(pid), Long.toString(available));
                }
                redis.setDataExpireBatch(toCache, TTL_SECONDS, TTL_JITTER_SECONDS);
            }
        }

        // 요청한 전체 목록 기준으로도 0 보정 보장(혹시 모를 누락 방지)
        Map<Long, Long> orderedOut = new LinkedHashMap<>(productIds.size());
        for (Long pid : productIds) {
            orderedOut.put(pid, result.getOrDefault(pid, 0L));
        }
        return orderedOut;
    }

    /** 단건 무효화 (특별 필요시에만; 기본 전략은 덮어쓰기) */
    public void invalidate(long productId) {
        redis.deleteData(cacheKey(productId));
    }

    /** 다건 무효화 (특별 필요시에만; 기본 전략은 덮어쓰기) */
    public void invalidateMany(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;
        Set<String> keys = new HashSet<>();
        for (Long pid : productIds) {
            if (pid != null) keys.add(cacheKey(pid));
        }
        if (!keys.isEmpty()) redis.deleteDataBatch(keys);
    }

    /**
     * 커밋 이후 최신값으로 바로 캐시 덮어쓰기(= 웜업)
     * - 무효화→미스 유도 방식 아님 → "0 보이는 공백" 방지
     * - 트랜잭션이 있으면 afterCommit, 없으면 즉시 실행
     * - 생성/재고변경/가격변경 등에 사용
     */
    public void refreshAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        Runnable work = () -> {
            // null 제거 + 중복 제거
            List<Long> ids = new ArrayList<>(new LinkedHashSet<>(productIds));
            for (int i = 0; i < ids.size(); i += CHUNK_SIZE) {
                List<Long> sub = ids.subList(i, Math.min(i + CHUNK_SIZE, ids.size()));
                Map<Long, Long> fromDb = stockRepository.findAvailableMapByProductIds(sub);

                Map<String, String> toCache = new LinkedHashMap<>(sub.size());
                for (Long pid : sub) {
                    long available = Math.max(0L, fromDb.getOrDefault(pid, 0L));
                    toCache.put(cacheKey(pid), Long.toString(available));
                }
                redis.setDataExpireBatch(toCache, TTL_SECONDS, TTL_JITTER_SECONDS);
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

    /**
     * ✅ 커밋 이후 캐시 삭제(EVICT)
     * - 상품 삭제/숨김(비활성화) 시 사용
     * - 트랜잭션이 있으면 afterCommit, 없으면 즉시 실행
     */
    public void evictAfterCommit(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) return;

        Runnable work = () -> {
            List<Long> ids = new ArrayList<>();
            for (Long id : productIds) if (id != null) ids.add(id);

            for (int i = 0; i < ids.size(); i += CHUNK_SIZE) {
                List<Long> sub = ids.subList(i, Math.min(i + CHUNK_SIZE, ids.size()));
                Set<String> keys = new HashSet<>(sub.size());
                for (Long pid : sub) keys.add(cacheKey(pid));
                if (!keys.isEmpty()) redis.deleteDataBatch(keys);
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

    public void evictAfterCommit(long productId) { evictAfterCommit(Collections.singletonList(productId)); }
}
