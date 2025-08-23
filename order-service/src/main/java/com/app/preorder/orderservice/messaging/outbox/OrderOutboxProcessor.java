package com.app.preorder.orderservice.messaging.outbox;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxProcessor {

    @PersistenceContext
    private EntityManager em;

    private final KafkaTemplate<String, StockRestoreRequest> kafkaTemplate;
    private final ObjectMapper om;

    /** 주기적으로 PENDING → Kafka 발송 → SENT 전이 */
    @Scheduled(fixedDelay = 700)
    @Transactional
    public void flush() {
        List<OrderOutboxEvent> batch = em.createQuery(
                        "select e from OrderOutboxEvent e where e.status = :st order by e.createdAt asc",
                        OrderOutboxEvent.class)
                .setParameter("st", OutboxStatus.PENDING)   // ★ 분리된 enum 사용
                .setMaxResults(100)
                .getResultList();

        for (OrderOutboxEvent e : batch) {
            try {
                StockRestoreRequest req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                kafkaTemplate.send(e.getTopic(), e.getPartitionKey(), req).get();
                e.markSent();                         // ✅ 세터 대신 도메인 메서드
            } catch (Exception ex) {
                log.warn("Outbox send failed id={}", e.getId(), ex);
                // 실패 시 PENDING 유지 → 다음 주기 재시도 (필요하면 FAILED/재시도 횟수 추가)
                // e.setStatus(OutboxStatus.FAILED);
            }
        }
    }
}
