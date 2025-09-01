package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxEvent;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxEventRepository;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final ObjectMapper objectMapper;
    private final OrderOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishStockRestoreRequest(Long orderId, List<StockRequestInternal> items) {
        Long partitionKey = items.isEmpty() ? orderId : items.get(0).getProductId();

        StockRestoreRequest payload = new StockRestoreRequest(
                orderId, items, UUID.randomUUID().toString(), partitionKey
        );
        try {
            String json = objectMapper.writeValueAsString(payload);

            OrderOutboxEvent event = OrderOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1)
                    .partitionKey(String.valueOf(partitionKey))
                    .payloadJson(json)
                    .status(OrderOutboxStatus.PENDING)
                    .build();

            outboxRepo.save(event);
        } catch (Exception e) {
            throw new RuntimeException("Outbox 적재 실패", e);
        }
    }
}
