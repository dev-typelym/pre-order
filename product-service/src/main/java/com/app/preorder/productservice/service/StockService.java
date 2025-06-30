package com.app.preorder.productservice.service;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.dto.StockInternal;

import java.util.List;

public interface StockService {

    List<StockInternal> getStocksByIds(List<Long> productIds); // ✅ 추가

    void deductStocks(List<StockRequestInternal> items);        // ✅ 추가

    void restoreStocks(List<StockRequestInternal> items);
}
