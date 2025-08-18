package com.app.preorder.productservice.messaging.producer;

public interface StockEventProducer {
    void sendStockChanged(long productId, long availableAfter);
    void sendSoldOut(long productId);
}
