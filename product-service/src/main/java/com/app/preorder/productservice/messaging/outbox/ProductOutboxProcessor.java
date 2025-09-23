package com.app.preorder.productservice.messaging.outbox;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.event.ProductStatusChangedEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private final KafkaTemplate<Object, Object> kafkaTemplate; // 기존 계약 유지
    private final ObjectMapper om;

    @Value("${outbox.product.publish-batch-size:1000}")
    private int publishBatchSize;

    /** NEW → (락 선점) → 전송 → SENT/FAILED */
    @Scheduled(fixedDelayString = "${outbox.product.publish-interval-ms:80}")
    @Transactional(noRollbackFor = Exception.class)
    public void flush() {
        // 1) NEW 배치 선점(행 잠금)
        List<Long> ids = outboxRepo.lockNewIds(publishBatchSize);
        if (ids.isEmpty()) return;

        // 2) 선점한 id만 로드
        List<ProductOutboxEvent> batch = outboxRepo.findByIdInOrderByIdAsc(ids);

        // 3) 전송 + 상태 마킹
        for (ProductOutboxEvent e : batch) {
            try {
                Object payload = switch (e.getTopic()) {
                    case KafkaTopics.INVENTORY_STOCK_EVENTS_V1 ->
                            om.readValue(e.getPayloadJson(), StockEvent.class);
                    case KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1 ->
                            om.readValue(e.getPayloadJson(), StockCommandResult.class);
                    case KafkaTopics.PRODUCT_STATUS_CHANGED_V1 ->
                            om.readValue(e.getPayloadJson(), ProductStatusChangedEvent.class);
                    default -> throw new IllegalArgumentException("지원되지 않는 토픽: " + e.getTopic());
                };
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();
            } catch (Exception ex) {
                log.warn("[Outbox][product] 전송 실패 id={}, topic={}, 이유={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }

        // 4) 일괄 저장
        outboxRepo.saveAll(batch);
    }
}
