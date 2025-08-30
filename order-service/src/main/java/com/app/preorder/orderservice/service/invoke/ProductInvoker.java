package com.app.preorder.orderservice.service.invoke;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.exception.custom.ProductCommandException;
import com.app.preorder.common.exception.custom.ProductQueryException;
import com.app.preorder.orderservice.client.ProductServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductInvoker {

    private final ProductServiceClient client;

    // ===== 조회 (필요 시 1회 재시도) =====
    @CircuitBreaker(name = "productQuery", fallbackMethod = "queryFallback")
    @Retry(name = "productQuery")
    public List<ProductInternal> getProductsByIds(List<Long> ids) {
        return client.getProductsByIds(ids);
    }

    @CircuitBreaker(name = "productQuery", fallbackMethod = "queryFallback")
    @Retry(name = "productQuery")
    public List<ProductInternal> getStocks(List<Long> ids) {
        return client.getStocks(ids);
    }

    private List<ProductInternal> queryFallback(List<Long> ids, Throwable ex) {
        throw new ProductQueryException("상품 조회 실패", ex);
    }

    // ===== 상태 변경 (재시도 금지) =====
    @CircuitBreaker(name = "productCommand", fallbackMethod = "commandFallback") // ← 이름 일치
    public void reserveStocks(List<StockRequestInternal> items) {
        client.reserveStocks(items);
    }

    @CircuitBreaker(name = "productCommand", fallbackMethod = "commandFallback")
    public void unreserveStocks(List<StockRequestInternal> items) {
        client.unreserveStocks(items);
    }

    @CircuitBreaker(name = "productCommand", fallbackMethod = "commandFallback")
    public void commitStocks(List<StockRequestInternal> items) {
        client.commitStocks(items);
    }

    @CircuitBreaker(name = "productCommand", fallbackMethod = "commandFallback")
    public void restoreStocks(List<StockRequestInternal> items) {
        client.restoreStocks(items);
    }

    private void commandFallback(List<StockRequestInternal> items, Throwable ex) {
        throw new ProductCommandException("상품 상태 변경(재고) 실패", ex);
    }
}
