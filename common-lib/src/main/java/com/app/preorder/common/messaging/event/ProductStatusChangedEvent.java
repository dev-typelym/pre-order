package com.app.preorder.common.messaging.event;

import com.app.preorder.common.type.ProductStatus;

public record ProductStatusChangedEvent(
        String eventId,
        String occurredAt,
        Long productId,
        ProductStatus status
) {}
