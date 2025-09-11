package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.*;
import com.app.preorder.common.messaging.event.OrderCompletedEvent;
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
public class KafkaOrderPublisher implements OrderCommandPublisher, OrderEventPublisher {

    private final ObjectMapper om;
    private final OrderOutboxEventRepository outboxRepo;

    @Override
    @Transactional
    public void publishReserveCommand(Long orderId, List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            enqueue(
                    KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1,
                    String.valueOf(it.getProductId()),
                    new ReserveStocksRequest(newId(), orderId, List.of(it), now())
            );
        }
    }

    @Override
    @Transactional
    public void publishCommitCommand(Long orderId, List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            enqueue(
                    KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1,
                    String.valueOf(it.getProductId()),
                    new CommitStocksRequest(newId(), orderId, List.of(it), now())
            );
        }
    }

    @Override
    @Transactional
    public void publishUnreserveCommand(Long orderId, List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            enqueue(
                    KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1,
                    String.valueOf(it.getProductId()),
                    new UnreserveStocksRequest(newId(), orderId, List.of(it), now())
            );
        }
    }

    @Override
    @Transactional
    public void publishStockRestoreCommand(Long orderId, List<StockRequestInternal> items) {
        for (StockRequestInternal it : items) {
            enqueue(
                    KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
                    String.valueOf(it.getProductId()),
                    new StockRestoreRequest(newId(), orderId, List.of(it), now())
            );
        }
    }

    // ✅ 주문 완료(결제 확정) 이벤트 발행 — 카트 정리 트리거는 memberId 기준 키 유지
    @Override
    @Transactional
    public void publishOrderCompleted(Long memberId, String orderType, List<Long> productIds) {
        var evt = new OrderCompletedEvent(newId(), now(), memberId, orderType, productIds);
        enqueue(KafkaTopics.ORDER_COMPLETED_V1, String.valueOf(memberId), evt);
    }

    // 기존 시그니처 유지 (Long orderId를 문자열 키로 변환)
    private void enqueue(String topic, Long orderId, Object payload) {
        enqueue(topic, String.valueOf(orderId), payload);
    }

    // 파티션 키를 문자열로 직접 지정할 수 있도록 오버로드
    private void enqueue(String topic, String partitionKey, Object payload) {
        try {
            String json = om.writeValueAsString(payload);
            outboxRepo.save(OrderOutboxEvent.builder()
                    .topic(topic)
                    .partitionKey(partitionKey)
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
