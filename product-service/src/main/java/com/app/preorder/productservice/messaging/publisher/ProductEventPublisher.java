package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.StockCommandResult;

public interface ProductEventPublisher {
    void publishStockChangedEvent(long productId, long availableAfter);
    void publishSoldOutEvent(long productId);
    void publishStockCommandResult(StockCommandResult result);}
