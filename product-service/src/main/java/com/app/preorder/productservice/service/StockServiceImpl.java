package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.common.exception.custom.InvalidStockRequestException;
import com.app.preorder.common.exception.custom.RestockFailedException;
import com.app.preorder.common.exception.custom.UnreserveFailedException;
import com.app.preorder.productservice.messaging.publisher.ProductEventPublisher;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final AvailableCacheService availableCache;
    private final ProductEventPublisher stockEvents;

    /* ========== 예약/해제(배치) ========== */

    // 결제 준비: 가용분 확인해 reserved 증가(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    public void reserveStocks(List<StockRequestInternal> items) {
        if (items == null || items.isEmpty())
            throw new InvalidStockRequestException("요청 항목(items)은 비어 있을 수 없습니다.");

        Set<Long> touched = new LinkedHashSet<>();
        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.reserve(pid, qty) != 1) {
                throw new InsufficientStockException("재고 부족(예약 실패): productId=" + pid + ", qty=" + qty);
            }
            touched.add(pid);
        }

        if (!touched.isEmpty()) {
            // 트랜잭션 내부에서 Outbox 적재(이벤트 발행 의도 기록) - 배치 가용재고 조회
            List<Long> pids = new ArrayList<>(touched);
            Map<Long, Long> availMap = stockRepository.findAvailableMapByProductIds(pids);

            for (Long pid : pids) {
                long available = Math.max(0L, availMap.getOrDefault(pid, 0L));
                stockEvents.publishStockChangedEvent(pid, available);
                if (available == 0) stockEvents.publishSoldOutEvent(pid);
            }

            // 커밋 이후 캐시 무효화 + 재계산(웜업)만 수행
            availableCache.refreshCacheAfterCommit(touched);
        }
    }

    // 이탈/취소: reserved 감소(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    public void unreserveStocks(List<StockRequestInternal> items) {
        if (items == null || items.isEmpty())
            throw new InvalidStockRequestException("요청 항목(items)은 비어 있을 수 없습니다.");

        Set<Long> touched = new LinkedHashSet<>();
        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.unreserve(pid, qty) != 1) {
                throw new UnreserveFailedException("예약 해제 실패: productId=" + pid + ", qty=" + qty);
            }
            touched.add(pid);
        }

        if (!touched.isEmpty()) {
            // 트랜잭션 내부에서 Outbox 적재 - 배치 가용재고 조회
            List<Long> pids = new ArrayList<>(touched);
            Map<Long, Long> availMap = stockRepository.findAvailableMapByProductIds(pids);

            for (Long pid : pids) {
                long available = Math.max(0L, availMap.getOrDefault(pid, 0L));
                stockEvents.publishStockChangedEvent(pid, available);
            }

            // 커밋 이후 캐시 무효화 + 재계산(웜업)
            availableCache.refreshCacheAfterCommit(touched);
        }
    }

    /* ========== 실제 차감(배치) ========== */

    // 결제 확정: qty와 reserved를 동일량 감소(원자), available 불변 → 이벤트 없음
    @Override
    public void commitStocks(List<StockRequestInternal> items) {
        if (items == null || items.isEmpty())
            throw new InvalidStockRequestException("요청 항목(items)은 비어 있을 수 없습니다.");

        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.commit(pid, qty) != 1) {
                throw new InsufficientStockException("커밋 실패: productId=" + pid + ", qty=" + qty);
            }
        }
        // available 불변 → 캐시/이벤트 불필요
    }

    /* ========== 보상/재입고(배치) ========== */

    // 재입고/보상: qty 증가(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    public void restoreStocks(List<StockRequestInternal> items) {
        if (items == null || items.isEmpty())
            throw new InvalidStockRequestException("요청 항목(items)은 비어 있을 수 없습니다.");

        Set<Long> touched = new LinkedHashSet<>();
        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.restock(pid, qty) != 1) {
                throw new RestockFailedException("재입고(보상) 실패: productId=" + pid + ", qty=" + qty);
            }
            touched.add(pid);
        }

        if (!touched.isEmpty()) {
            // 트랜잭션 내부에서 Outbox 적재 - 배치 가용재고 조회
            List<Long> pids = new ArrayList<>(touched);
            Map<Long, Long> availMap = stockRepository.findAvailableMapByProductIds(pids);

            for (Long pid : pids) {
                long available = Math.max(0L, availMap.getOrDefault(pid, 0L));
                stockEvents.publishStockChangedEvent(pid, available);
            }

            // 커밋 이후 캐시 무효화 + 재계산(웜업)
            availableCache.refreshCacheAfterCommit(touched);
        }
    }
}
