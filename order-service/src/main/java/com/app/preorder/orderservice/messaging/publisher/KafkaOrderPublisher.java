package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.*;
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
public class KafkaOrderPublisher implements OrderCommandPublisher {

    private final ObjectMapper om;
    private final OrderOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishReserveCommand(Long orderId, List<StockRequestInternal> items) {
        enqueue(KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1,
                orderId,
                new ReserveStocksRequest(newId(), orderId, items, now()));
    }

    @Override
    @Transactional
    public void publishCommitCommand(Long orderId, List<StockRequestInternal> items) {
        enqueue(KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1,
                orderId,
                new CommitStocksRequest(newId(), orderId, items, now()));
    }

    @Override
    @Transactional
    public void publishUnreserveCommand(Long orderId, List<StockRequestInternal> items) {
        enqueue(KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1,
                orderId,
                new UnreserveStocksRequest(newId(), orderId, items, now()));
    }

    @Override
    @Transactional
    public void publishStockRestoreCommand(Long orderId, List<StockRequestInternal> items) {
        enqueue(KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
                orderId,
                new StockRestoreRequest(newId(), orderId, items, now()));
    }

    private void enqueue(String topic, Long orderId, Object payload) {
        try {
            String json = om.writeValueAsString(payload);
            outboxRepo.save(OrderOutboxEvent.builder()
                    .topic(topic)
                    .partitionKey(String.valueOf(orderId))
                    .payloadJson(json)
                    .status(OutboxStatus.NEW)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Outbox 적재 실패", e);
        }
    }

    private static String newId() { return UUID.randomUUID().toString(); }
    private static String now()   { return Instant.now().toString(); }
}
