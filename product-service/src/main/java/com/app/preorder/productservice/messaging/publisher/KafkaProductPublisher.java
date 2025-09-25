package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.ProductStatusChangedEvent;
import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.ProductStatus;
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
                UUID.randomUUID().toString(),   // message_key
                "STOCK_CHANGED",
                productId,
                availableAfter,
                Instant.now().toString()
        );
        try {
            String json = om.writeValueAsString(evt);
            outboxRepo.upsertNew(
                    evt.eventId(),                                // message_key(멱등키)
                    KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
                    String.valueOf(productId),                    // partition_key
                    json
            );
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 업서트 실패(StockEvent: STOCK_CHANGED)", e);
        }
    }

    @Override
    @Transactional
    public void publishSoldOutEvent(long productId) {
        var evt = new StockEvent(
                UUID.randomUUID().toString(),   // message_key
                "SOLD_OUT",
                productId,
                0L,
                Instant.now().toString()
        );
        try {
            String json = om.writeValueAsString(evt);
            outboxRepo.upsertNew(
                    evt.eventId(),
                    KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
                    String.valueOf(productId),
                    json
            );
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 업서트 실패(StockEvent: SOLD_OUT)", e);
        }
    }

    @Override
    @Transactional
    public void publishStockCommandResult(StockCommandResult result) {
        try {
            String json = om.writeValueAsString(result);
            outboxRepo.upsertNew(
                    result.eventId(),                               // 요청 eventId = 멱등키
                    KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1,
                    String.valueOf(result.orderId()),
                    json
            );
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 업서트 실패(StockCommandResult)", e);
        }
    }

    @Override
    @Transactional
    public void publishProductStatusChanged(long productId, ProductStatus status) {
        var evt = new ProductStatusChangedEvent(
                UUID.randomUUID().toString(),   // message_key
                Instant.now().toString(),
                productId,
                status
        );
        try {
            String json = om.writeValueAsString(evt);
            outboxRepo.upsertNew(
                    evt.eventId(),
                    KafkaTopics.PRODUCT_STATUS_CHANGED_V1,
                    String.valueOf(productId),
                    json
            );
        } catch (Exception e) {
            throw new RuntimeException("ProductOutbox 업서트 실패(ProductStatusChanged)", e);
        }
    }
}
