package com.app.preorder.common.messaging.event;

import java.util.UUID;

public record MemberRegisterEvent(
        UUID eventId,
        Long memberId,
        String loginId,     // 선택(감사/로그용)
        String email,       // 선택(가능하면 최소화)
        String occurredAt   // ISO-8601 UTC 문자열 (Instant.now().toString())
) {}