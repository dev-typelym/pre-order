package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;

import java.util.List;

public interface OrderCommandPublisher {
    /** 동기 복원 실패 시, 재고 복원을 비동기로 요청한다. */
    void publishStockRestoreCommand(Long orderId, List<StockRequestInternal> items);
}
