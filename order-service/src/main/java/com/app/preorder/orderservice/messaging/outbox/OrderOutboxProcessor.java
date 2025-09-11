package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.messaging.command.*;
import com.app.preorder.common.messaging.event.OrderCompletedEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxProcessor {

    private final OrderOutboxEventRepository outboxRepo;
    private final ObjectMapper om;
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelayString = "${outbox.order.publish-interval-ms:700}")
    @Transactional
    public void flush() {
        List<OrderOutboxEvent> batch =
                outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);

        for (OrderOutboxEvent e : batch) {
            try {
                Object payload = switch (e.getTopic()) {
                    case KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1   -> om.readValue(e.getPayloadJson(), ReserveStocksRequest.class);
                    case KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1    -> om.readValue(e.getPayloadJson(), CommitStocksRequest.class);
                    case KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1 -> om.readValue(e.getPayloadJson(), UnreserveStocksRequest.class);
                    case KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1   -> om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                    case KafkaTopics.ORDER_COMPLETED_V1                   -> om.readValue(e.getPayloadJson(), OrderCompletedEvent.class);
                    default -> throw new IllegalArgumentException("지원되지 않는 토픽입니다: " + e.getTopic());
                };

                // 전송 (성공 로그 없음)
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();

            } catch (Exception ex) {
                log.warn("[Outbox][order] 전송 실패 id={}, topic={}, 사유={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
