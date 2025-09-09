package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OutboxStatus;
import com.app.preorder.productservice.messaging.outbox.ProductOutboxEvent;
import com.app.preorder.productservice.messaging.outbox.ProductOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class KafkaProductPublisher implements ProductEventPublisher {

    private final ObjectMapper om;
    private final ProductOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishStockChangedEvent(long productId, long availableAfter) {
        try {
            var evt  = new StockEvent("STOCK_CHANGED", productId, availableAfter, Instant.now().toString());
            var json = om.writeValueAsString(evt);

            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_EVENTS_V1)
                    .partitionKey(String.valueOf(productId)) // 파티션 키 = productId
                    .payloadJson(json)
                    .status(com.app.preorder.common.type.OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("프로덕트 아웃박스 적재 실패", e);
        }
    }

    @Override
    @Transactional
    public void publishSoldOutEvent(long productId) {
        try {
            var evt  = new StockEvent("SOLD_OUT", productId, 0L, Instant.now().toString());
            var json = om.writeValueAsString(evt);

            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_EVENTS_V1)
                    .partitionKey(String.valueOf(productId))
                    .payloadJson(json)
                    .status(com.app.preorder.common.type.OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("프로덕트 아웃박스 적재 실패", e);
        }
    }

    @Override
    @Transactional
    public void publishStockCommandResult(StockCommandResult result) {
        try {
            String json = om.writeValueAsString(result);
            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1)
                    .partitionKey(String.valueOf(result.orderId()))
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 적재 실패(StockCommandResult)", e);
        }
    }
}

