package com.app.preorder.productservice.messaging.publisher;

public interface ProductEventPublisher {
    void publishStockChanged(long productId, long availableAfter);
    void publishSoldOut(long productId);
}
