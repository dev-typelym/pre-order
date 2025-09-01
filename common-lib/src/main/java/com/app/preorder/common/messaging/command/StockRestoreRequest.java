package com.app.preorder.common.messaging.command;

import com.app.preorder.common.dto.StockRequestInternal;

import java.util.List;

public record StockRestoreRequest(
        String eventId,                 // UUID
        Long orderId,                   // aggregateId
        List<StockRequestInternal> items,
        String occurredAt               // ISO-8601 (Instant.now().toString())
) {}
