package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.type.ProductStatus; // ✅ 추가

public interface ProductEventPublisher {
    void publishStockChangedEvent(long productId, long availableAfter);
    void publishSoldOutEvent(long productId);
    void publishStockCommandResult(StockCommandResult result);
    void publishProductStatusChanged(long productId, ProductStatus status);
}
