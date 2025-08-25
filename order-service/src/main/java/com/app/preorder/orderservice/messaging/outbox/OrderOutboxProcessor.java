package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
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
        List<OrderOutboxEvent> batch =
                outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OrderOutboxStatus.PENDING);

        for (OrderOutboxEvent e : batch) {
            try {
                StockRestoreRequest req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), req).get();
                e.markSent(); // 더티체킹으로 UPDATE
            } catch (Exception ex) {
                log.warn("Outbox 전송 실패 id={}", e.getId(), ex);
                // 필요하면 정책에 따라 e.markFailed(); 추가
            }
        }
        // @Transactional 이라 루프 종료 시점에 한 번 flush/commit
    }
}
