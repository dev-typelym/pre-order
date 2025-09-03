package com.app.preorder.common.messaging.command;

import com.app.preorder.common.dto.StockRequestInternal;

import java.util.List;

public record CommitStocksRequest(
        String eventId,
        Long orderId,
        List<StockRequestInternal> items,
        String occurredAt
) {}
