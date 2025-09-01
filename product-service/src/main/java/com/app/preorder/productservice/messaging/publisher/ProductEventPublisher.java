package com.app.preorder.productservice.messaging.publisher;

public interface ProductEventPublisher {
    void publishStockChangedEvent(long productId, long availableAfter);
    void publishSoldOutEvent(long productId);
}
