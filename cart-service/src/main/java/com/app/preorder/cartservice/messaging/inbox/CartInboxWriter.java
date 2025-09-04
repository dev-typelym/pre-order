package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartInboxWriter {

    private final CartInboxEventRepository repo;
    private final ObjectMapper om;

    @Transactional
    @KafkaListener(
            id = "cart-inbox-writer",
            topics = KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1,
            groupId = "cart-inbox-writer",
            containerFactory = "cartCreateListenerContainerFactory"
    )
    public void onCartCreate(CartCreateRequest cmd) {
        try {
            // 멱등: 이미 처리된 이벤트면 무시
            if (repo.existsByMessageKey(cmd.eventId())) {
                return;
            }
            String json = om.writeValueAsString(cmd);
            repo.save(CartInboxEvent.of(
                    cmd.eventId(),
                    KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1,
                    json
            ));
        } catch (Exception e) {
            // unique 위반은 errorHandler가 비재시도 처리
            throw new RuntimeException("Inbox write failed", e);
        }
    }
}
