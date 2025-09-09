package com.app.preorder.memberservice.messaging.publisher;

public interface MemberEventPublisher {
    void publishMemberDeactivated(Long memberId);
}
