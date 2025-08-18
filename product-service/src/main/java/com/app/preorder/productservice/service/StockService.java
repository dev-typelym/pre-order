package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.dto.StockInternal;

import java.util.List;

public interface StockService {

    List<StockInternal> getStocksByIds(List<Long> productIds);

    // ✅ 추가: 예약/해제 배치
    void reserveStocks(List<StockRequestInternal> items);
    void unreserveStocks(List<StockRequestInternal> items);
    void commitStocks(List<StockRequestInternal> items);

    // ⛔ 의미가 ‘재입고’라면, 아래 구현에서 원자 restock으로 바꿔둠
    void restoreStocks(List<StockRequestInternal> items);
}
