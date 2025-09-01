package com.app.preorder.common.messaging.command;

public record CartCreateRequest(
        String eventId,      // UUID
        Long memberId,
        String loginId,
        String email,
        String occurredAt // ISO-8601 UTC 문자열 (Instant.now().toString())
) {}