package com.app.preorder.common.messaging.event;

import java.util.List;

public record OrderCompletedEvent(
        String eventId,
        String occurredAt,
        Long memberId,
        String orderType,          // "CART" | "BUY_NOW"
        List<Long> productIds   // 카트 비우기 목적: productId만
) {}
