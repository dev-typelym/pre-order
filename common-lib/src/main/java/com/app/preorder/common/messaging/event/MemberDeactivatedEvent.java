package com.app.preorder.common.messaging.event;


public record MemberDeactivatedEvent(
        String eventId,    // UUID
        String occurredAt,
        Long memberId
) {}
