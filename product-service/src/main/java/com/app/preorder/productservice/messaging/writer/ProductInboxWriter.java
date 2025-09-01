package com.app.preorder.productservice.messaging.writer;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.productservice.messaging.inbox.ProductInboxEvent;
import com.app.preorder.productservice.messaging.inbox.ProductInboxEventRepository;
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

    /** 재고 복원 커맨드 수신 → Inbox(PENDING) 적재(멱등) */
    @Transactional
    @KafkaListener(
            id = "product-inbox-writer",
            topics = KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
            groupId = "product-inbox-writer",
            containerFactory = "stockRestoreKafkaListenerContainerFactory" // 네가 쓰던 팩토리 유지
    )
    public void write(StockRestoreRequest req) {
        // 멱등: eventId로 이미 적재되어 있으면 바로 리턴
        if (inboxRepo.findByMessageKey(req.eventId()).isPresent()) return;

        try {
            String payloadJson = om.writeValueAsString(req);
            inboxRepo.save(ProductInboxEvent.of(
                    req.eventId(),
                    KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
                    payloadJson
            ));
        } catch (DataIntegrityViolationException dup) {
            // UNIQUE(message_key) 충돌 → 이미 적재됨
        } catch (Exception e) {
            log.warn("[Inbox][product] 적재 실패 eventId={}, reason={}", req.eventId(), e.toString());
            throw new RuntimeException("Inbox 적재 실패", e);
        }
    }
}

