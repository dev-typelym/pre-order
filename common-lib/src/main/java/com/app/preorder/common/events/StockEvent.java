package com.app.preorder.common.events;

public record StockEvent(
        String type,        // "STOCK_CHANGED" | "SOLD_OUT" ... (확장 여지)
        long productId,
        Long available,     // null 허용 (타입별로 없을 수 있음)
        String occurredAt   // ISO-8601 (Instant.now().toString())
) {}
