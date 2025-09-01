package com.app.preorder.memberservice.messaging.publisher;

public interface MemberCommandPublisher {
    void publishCartCreateCommand(Long memberId, String loginId, String email);
}
