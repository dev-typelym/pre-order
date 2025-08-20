package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.common.exception.custom.InvalidStockRequestException;
import com.app.preorder.common.exception.custom.RestockFailedException;
import com.app.preorder.common.exception.custom.UnreserveFailedException;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.messaging.producer.StockEventProducer;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductFactory productFactory;
    private final AvailableCacheService availableCache;
    private final StockEventProducer stockEvents;

    // 트랜잭션 커밋 이후에만 콜백 실행(이벤트 발행 시점 보호)
    private void afterCommit(Runnable r) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { r.run(); }
        });
    }

    // productId 목록으로 재고 조회 후 내부 DTO(StockInternal)로 변환
    @Override
    @Transactional(readOnly = true)
    public List<StockInternal> getStocksByIds(List<Long> productIds) {
        List<Stock> stocks = stockRepository.findByProductIds(productIds);
        return stocks.stream()
                .map(productFactory::toStockInternal)
                .toList();
    }

    /* ========== 예약/해제(배치) ========== */

    // 결제 준비: 가용분 확인해 reserved 증가(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    @Transactional
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
            afterCommit(() -> {
                availableCache.invalidateMany(touched);
                var av = availableCache.getMany(List.copyOf(touched));
                for (Long pid : touched) {
                    long available = av.getOrDefault(pid, 0L);
                    stockEvents.sendStockChanged(pid, available);
                    if (available == 0) stockEvents.sendSoldOut(pid);
                }
            });
        }
    }

    // 이탈/취소: reserved 감소(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    @Transactional
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
            afterCommit(() -> {
                availableCache.invalidateMany(touched);
                var av = availableCache.getMany(List.copyOf(touched));
                for (Long pid : touched) {
                    stockEvents.sendStockChanged(pid, av.getOrDefault(pid, 0L));
                }
            });
        }
    }

    /* ========== 실제 차감(배치) ========== */

    // 결제 확정: qty와 reserved를 동일량 감소(원자), available 불변 → 이벤트 없음
    @Override
    @Transactional
    public void commitStocks(List<StockRequestInternal> items) {
        if (items == null || items.isEmpty())
            throw new InvalidStockRequestException("요청 항목(items)은 비어 있을 수 없습니다.");

        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.consumeReserved(pid, qty) != 1) {
                throw new InsufficientStockException("커밋 실패: productId=" + pid + ", qty=" + qty);
            }
        }
    }

    /* ========== 보상/재입고(배치) ========== */

    // 재입고/보상: qty 증가(원자), 커밋 후 재고 변경 이벤트 발행
    @Override
    @Transactional
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
            afterCommit(() -> {
                availableCache.invalidateMany(touched);
                var av = availableCache.getMany(List.copyOf(touched));
                for (Long pid : touched) {
                    stockEvents.sendStockChanged(pid, av.getOrDefault(pid, 0L));
                }
            });
        }
    }
}
