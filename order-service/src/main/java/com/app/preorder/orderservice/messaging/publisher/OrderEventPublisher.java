package com.app.preorder.orderservice.messaging.publisher;

import java.util.List;

public interface OrderEventPublisher {

    void publishOrderCompleted(Long memberId, String orderType, List<Long> productIds);
}