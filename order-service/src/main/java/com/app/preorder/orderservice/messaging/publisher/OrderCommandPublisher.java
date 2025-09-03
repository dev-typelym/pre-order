package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;

import java.util.List;

public interface OrderCommandPublisher {
    void publishReserveCommand(Long orderId, List<StockRequestInternal> items);
    void publishCommitCommand(Long orderId, List<StockRequestInternal> items);
    void publishUnreserveCommand(Long orderId, List<StockRequestInternal> items);
    void publishStockRestoreCommand(Long orderId, List<StockRequestInternal> items);
}
