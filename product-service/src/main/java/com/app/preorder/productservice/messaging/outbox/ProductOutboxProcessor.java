package com.app.preorder.productservice.messaging.outbox;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.event.StockEvent;
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
public class ProductOutboxProcessor {

    private final ProductOutboxEventRepository outboxRepo;
    /** 제네릭 템플릿 */
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${outbox.product.publish-interval-ms:700}")
    @Transactional
    public void flush() {
        List<ProductOutboxEvent> batch =
                outboxRepo.findTop100ByStatusOrderByIdAsc(OutboxStatus.NEW);

        for (ProductOutboxEvent e : batch) {
            try {
                Object payload = switch (e.getTopic()) {
                    case KafkaTopics.INVENTORY_STOCK_EVENTS_V1            -> om.readValue(e.getPayloadJson(), StockEvent.class);
                    case KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1   -> om.readValue(e.getPayloadJson(), StockCommandResult.class);
                    default -> throw new IllegalArgumentException("지원되지 않는 토픽입니다: " + e.getTopic());
                };
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();
            } catch (Exception ex) {
                log.warn("[Outbox][product] 전송 실패 id={}, topic={}, 사유={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString()); // ★ 실패 사유 기록
            }
        }
    }
}