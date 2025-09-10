package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.event.ProductStatusChangedEvent; // ✅ 추가
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OutboxStatus;
import com.app.preorder.common.type.ProductStatus; // ✅ 추가
import com.app.preorder.productservice.messaging.outbox.ProductOutboxEvent;
import com.app.preorder.productservice.messaging.outbox.ProductOutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaProductPublisher implements ProductEventPublisher {

    private final ObjectMapper om;
    private final ProductOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishStockChangedEvent(long productId, long availableAfter) {
        var evt = new StockEvent(
                UUID.randomUUID().toString(),  // ✅ eventId
                "STOCK_CHANGED",
                productId,
                availableAfter,
                Instant.now().toString()
        );
        try {
            String json = om.writeValueAsString(evt);
            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_EVENTS_V1)
                    .partitionKey(String.valueOf(productId))
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 적재 실패(StockEvent: STOCK_CHANGED)", e);
        }
    }

    @Override
    @Transactional
    public void publishSoldOutEvent(long productId) {
        var evt = new StockEvent(
                UUID.randomUUID().toString(),  // ✅ eventId
                "SOLD_OUT",
                productId,
                0L,                            // 품절이므로 available=0
                Instant.now().toString()
        );
        try {
            String json = om.writeValueAsString(evt);
            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.INVENTORY_STOCK_EVENTS_V1)
                    .partitionKey(String.valueOf(productId))
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 적재 실패(StockEvent: SOLD_OUT)", e);
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

    @Override
    @Transactional
    public void publishProductStatusChanged(long productId, ProductStatus status) {
        try {
            var evt = new ProductStatusChangedEvent(
                    UUID.randomUUID().toString(),
                    Instant.now().toString(),
                    productId,
                    status
            );
            String json = om.writeValueAsString(evt);
            outboxRepo.save(ProductOutboxEvent.builder()
                    .topic(KafkaTopics.PRODUCT_STATUS_CHANGED_V1)
                    .partitionKey(String.valueOf(productId))
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 적재 실패(ProductStatusChanged)", e);
        }
    }
}
