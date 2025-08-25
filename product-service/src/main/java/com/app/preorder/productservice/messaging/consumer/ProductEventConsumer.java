package com.app.preorder.productservice.messaging.consumer;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.messaging.inbox.ProductInboxEvent;
import com.app.preorder.productservice.messaging.inbox.ProductInboxEventRepository;
import com.app.preorder.productservice.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductInboxEventRepository inboxRepo;
    private final ProductRepository productRepository;
    private final ObjectMapper om;

    // 한 줄 주석: 재고 복원 요청 수신 → Inbox(PENDING) 적재만
    @Transactional
    @KafkaListener(
            id = "stockRestoreInboxWriter",
            topics = KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
            groupId = "product-inbox-writer"
    )
    public void onStockRestoreRequest(StockRestoreRequest req) throws Exception {
        String json = om.writeValueAsString(req);
        ProductInboxEvent inbox = ProductInboxEvent.of(
                req.eventId(),
                KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
                json
        );

        try {
            inboxRepo.save(inbox); // UNIQUE(messageKey)로 중복 차단
        } catch (DataIntegrityViolationException ignore) {
            // 이미 들어온 메시지면 무시(멱등)
        }
    }

    // 한 줄 주석: 읽기모델 동기화 이벤트는 즉시 처리(인박스 비대상)
    @Transactional
    @KafkaListener(
            id = "stockEventsConsumer",
            topics = KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
            groupId = "product-readmodel"
    )
    public void onStockEvent(StockEvent evt) {
        switch (evt.type()) {
            case "SOLD_OUT" -> productRepository.updateStatus(evt.productId(), ProductStatus.SOLD_OUT);
            case "STOCK_CHANGED" -> {
                Long a = evt.available();
                productRepository.updateStatus(
                        evt.productId(),
                        (a != null && a > 0) ? ProductStatus.ENABLED : ProductStatus.SOLD_OUT
                );
            }
            default -> { /* ignore */ }
        }
    }
}
