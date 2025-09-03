package com.app.preorder.orderservice.messaging.inbox;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderInboxWriter {

    private final OrderInboxEventRepository repo;
    private final ObjectMapper om;

    /** 결과 이벤트 수신 → Inbox 적재(멱등) */
    @Transactional
    @KafkaListener(
            id = "order-inbox-writer-stock-result",
            topics = KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1,
            groupId = "order-service",
            containerFactory = "stockCommandResultKafkaListenerContainerFactory"
    )
    public void write(StockCommandResult r) {
        if (repo.existsByMessageKey(r.eventId())) return; // 멱등
        try {
            String json = om.writeValueAsString(r);
            repo.save(OrderInboxEvent.of(
                    r.eventId(),
                    KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1,
                    json
            ));
        } catch (DataIntegrityViolationException dup) {
            // 동시성 등으로 이미 적재된 케이스
        } catch (Exception e) {
            log.warn("[Inbox][order] 적재 실패 eventId={}, 사유={}", r.eventId(), e.toString());
            throw new RuntimeException("OrderInbox 적재 실패", e);
        }
    }
}
