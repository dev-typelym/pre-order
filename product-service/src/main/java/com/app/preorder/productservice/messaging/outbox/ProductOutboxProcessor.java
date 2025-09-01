package com.app.preorder.productservice.messaging.outbox;

import com.app.preorder.common.messaging.event.StockEvent;
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
    private final KafkaTemplate<String, StockEvent> kafkaTemplate;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${outbox.product.publish-interval-ms:700}")
    @Transactional
    public void flush() {
        List<ProductOutboxEvent> batch =
                outboxRepo.findTop100ByStatusOrderByIdAsc(OutboxStatus.NEW);
        for (ProductOutboxEvent e : batch) {
            try {
                StockEvent payload = om.readValue(e.getPayloadJson(), StockEvent.class);
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), payload).get();
                e.markSent();
            } catch (Exception ex) {
                log.warn("[Outbox][product] 전송 실패 id={}, topic={}, reason={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed();
            }
        }
    }
}
