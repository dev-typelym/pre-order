package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
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
    private final KafkaTemplate<String, StockRestoreRequest> kafkaTemplate;
    private final ObjectMapper om;

    @Scheduled(fixedDelay = 700)
    @Transactional
    public void flush() {
        var batch = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.NEW);
        for (var e : batch) {
            try {
                var req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), req).get();
                e.markSent();
            } catch (Exception ex) {
                log.warn("Outbox 전송 실패 id={}, reason={}", e.getId(), ex.toString());
                e.markFailed(); // 실패 표시(무한 재시도 루프 방지)
            }
        }
    }
}
