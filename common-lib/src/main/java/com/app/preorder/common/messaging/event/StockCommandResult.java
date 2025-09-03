package com.app.preorder.common.messaging.event;

import com.app.preorder.common.type.StockCommandResultType;

public record StockCommandResult(
        String eventId,          // 커맨드 eventId 재사용 권장
        Long orderId,            // 어떤 주문 결과인지
        StockCommandResultType result,
        String reason,           // 실패 사유(성공이면 null)
        String occurredAt        // ISO-8601
) {}
