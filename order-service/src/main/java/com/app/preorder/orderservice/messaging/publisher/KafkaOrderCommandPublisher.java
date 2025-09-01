package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OutboxStatus;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxEvent;
import com.app.preorder.orderservice.messaging.outbox.OrderOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaOrderCommandPublisher implements OrderCommandPublisher {

    private final ObjectMapper om;
    private final OrderOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishStockRestoreCommand(Long orderId, List<StockRequestInternal> items) {
        try {
            var cmd = new StockRestoreRequest(
                    UUID.randomUUID().toString(),
                    orderId,
                    items,
                    Instant.now().toString()
            );
            var json = om.writeValueAsString(cmd);

            outboxRepo.save(OrderOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1)
                    .partitionKey(String.valueOf(orderId))   // ★ 키는 여기(Outbox 컬럼)만
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Outbox 적재 실패", e);
        }
    }
}

