// product-service/src/main/java/com/app/preorder/productservice/messaging/writer/ProductInboxWriter.java
package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.messaging.command.*;
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
public class ProductInboxWriter {

    private final ProductInboxEventRepository inboxRepo;
    private final ObjectMapper om;

    /** RESTORE */
    @Transactional
    @KafkaListener(
            id = "product-inbox-writer-restore",
            topics = KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
            groupId = "product-inbox-writer",
            containerFactory = "stockRestoreKafkaListenerContainerFactory"
    )
    public void onRestore(StockRestoreRequest req) {
        writeInbox(req.eventId(), KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1, req);
    }

    /** RESERVE */
    @Transactional
    @KafkaListener(
            id = "product-inbox-writer-reserve",
            topics = KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1,
            groupId = "product-inbox-writer",
            containerFactory = "stockReserveKafkaListenerContainerFactory"
    )
    public void onReserve(ReserveStocksRequest req) {
        writeInbox(req.eventId(), KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1, req);
    }

    /** COMMIT */
    @Transactional
    @KafkaListener(
            id = "product-inbox-writer-commit",
            topics = KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1,
            groupId = "product-inbox-writer",
            containerFactory = "stockCommitKafkaListenerContainerFactory"
    )
    public void onCommit(CommitStocksRequest req) {
        writeInbox(req.eventId(), KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1, req);
    }

    /** UNRESERVE */
    @Transactional
    @KafkaListener(
            id = "product-inbox-writer-unreserve",
            topics = KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1,
            groupId = "product-inbox-writer",
            containerFactory = "stockUnreserveKafkaListenerContainerFactory"
    )
    public void onUnreserve(UnreserveStocksRequest req) {
        writeInbox(req.eventId(), KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1, req);
    }

    // ---- private helpers (보통 아래로) ----
    private void writeInbox(String eventId, String topic, Object payload) {
        try {
            // 사전 멱등 체크: existsBy... 사용으로 불필요한 엔티티 로딩 회피
            if (inboxRepo.existsByMessageKey(eventId)) return;

            String payloadJson = om.writeValueAsString(payload);
            inboxRepo.save(ProductInboxEvent.of(eventId, topic, payloadJson));
        } catch (DataIntegrityViolationException dup) {
            // UNIQUE(message_key) 충돌 → 동시성/재처리에서 이미 적재됨
        } catch (Exception e) {
            log.warn("[Inbox][product] 적재 실패 eventId={}, topic={}, reason={}", eventId, topic, e.toString());
            throw new RuntimeException("Inbox 적재 실패", e);
        }
    }
}
