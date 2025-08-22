package com.app.preorder.common.messaging.command;

import com.app.preorder.common.dto.StockRequestInternal;

import java.util.List;

public record StockRestoreRequest(
        Long orderId,
        List<StockRequestInternal> items,
        String eventId,    // UUID
        Long partitionKey  // 보통 첫 productId
) {}
