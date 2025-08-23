package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxEvent;
import com.app.preorder.orderservice.messaging.outbox.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    @PersistenceContext
    private EntityManager em;

    private final ObjectMapper om;

    @Override
    public void publishStockRestoreRequest(Long orderId, List<StockRequestInternal> items) {
        Long partitionKey = items.isEmpty() ? orderId : items.get(0).getProductId();

        StockRestoreRequest payload = new StockRestoreRequest(
                orderId,
                items,
                UUID.randomUUID().toString(),  // 멱등키
                partitionKey
        );

        try {
            String json = om.writeValueAsString(payload);

            em.persist(
                    OrderOutboxEvent.builder()
                            .topic(KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1)
                            .partitionKey(String.valueOf(partitionKey))
                            .payloadJson(json)
                            .status(OutboxStatus.PENDING)
                            .build()
            );
            // 같은 트랜잭션으로 커밋 → 프로세서가 PENDING을 읽어 Kafka 발송
        } catch (Exception e) {
            throw new RuntimeException("Outbox persist failed", e);
        }
    }
}
