package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.dto.StockInternal;
import com.app.preorder.common.exception.custom.InsufficientStockException;
import com.app.preorder.productservice.domain.entity.Stock;
import com.app.preorder.productservice.messaging.producer.StockEventProducer;
import com.app.preorder.productservice.factory.ProductFactory;
import com.app.preorder.productservice.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final ProductFactory productFactory;
    private final StockEventProducer stockEvents;

    @Override
    @Transactional(readOnly = true)
    public List<StockInternal> getStocksByIds(List<Long> productIds) {
        List<Stock> stocks = stockRepository.findByProductIds(productIds);
        return stocks.stream()
                .map(productFactory::toStockInternal)
                .toList();
    }

    /* ========== 예약/해제(배치) ========== */

    @Override
    @Transactional
    public void reserveStocks(List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            long pid = it.getProductId();
            long qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.reserve(pid, qty) != 1) {
                throw new InsufficientStockException("재고 부족(예약 실패): productId=" + pid + ", qty=" + qty);
            }
            // ✅ 이벤트 발행
            long available = stockRepository.findAvailable(pid).orElse(0L);
            stockEvents.sendStockChanged(pid, available);
        }
    }

    @Override
    @Transactional
    public void unreserveStocks(List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            long pid = it.getProductId();
            long qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.unreserve(pid, qty) != 1) {
                throw new IllegalStateException("예약 해제 실패: productId=" + pid + ", qty=" + qty);
            }
            // ✅ 이벤트 발행
            long available = stockRepository.findAvailable(pid).orElse(0L);
            stockEvents.sendStockChanged(pid, available);
        }
    }

    /* ========== 실제 차감(배치) ========== */

    @Override
    @Transactional
    public void commitStocks(List<StockRequestInternal> items) {
        for (var it : items) {
            long pid = it.getProductId(), qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.consumeReserved(pid, qty) != 1) {
                throw new InsufficientStockException("커밋 실패: productId=" + pid + ", qty=" + qty);
            }
            // ✅ 이벤트 발행
            long available = stockRepository.findAvailable(pid).orElse(0L);
            stockEvents.sendStockChanged(pid, available);
            if (available == 0) {
                stockEvents.sendSoldOut(pid);
            }
        }
        // 품절 상태 전환을 “이벤트 소비자”에서 하려면, 위의 SOLD_OUT만 보내고
        // 여기선 상태 변경을 하지 않는 게 더 깔끔함.
    }

    /* ========== 보상/재입고(배치) ========== */
    @Override
    @Transactional
    public void restoreStocks(List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            long pid = it.getProductId();
            long qty = it.getQuantity();
            if (qty <= 0) continue;

            if (stockRepository.restock(pid, qty) != 1) {
                throw new IllegalStateException("재입고(보상) 실패: productId=" + pid + ", qty=" + qty);
            }
            // ✅ 이벤트 발행
            long available = stockRepository.findAvailable(pid).orElse(0L);
            stockEvents.sendStockChanged(pid, available);
        }
    }
}