package com.app.preorder.memberservice.messaging.publisher;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OutboxStatus;
import com.app.preorder.memberservice.messaging.outbox.MemberOutboxEvent;
import com.app.preorder.memberservice.messaging.outbox.MemberOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaMemberCommandPublisher implements MemberCommandPublisher {

    private final MemberOutboxEventRepository outboxRepo;
    private final ObjectMapper om;

    @Override
    @Transactional
    public void publishCartCreateCommand(Long memberId, String loginId, String email) {
        try {
            var cmd = new CartCreateRequest(
                    UUID.randomUUID().toString(),
                    memberId,
                    loginId,
                    email,
                    Instant.now().toString()
            );
            String payloadJson = om.writeValueAsString(cmd);

            outboxRepo.save(
                    MemberOutboxEvent.builder()
                            .topic(KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1)
                            .partitionKey(String.valueOf(memberId)) // key=memberId
                            .payloadJson(payloadJson)
                            .status(OutboxStatus.NEW)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("publishCartCreate enqueue 실패", e);
        }
    }
}

