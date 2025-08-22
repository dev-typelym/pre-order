package com.app.preorder.productservice.messaging.consumer;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.messaging.idempotency.ProcessedEvent;
import com.app.preorder.productservice.messaging.idempotency.ProcessedEventRepository;
import com.app.preorder.productservice.repository.ProductRepository;
import com.app.preorder.productservice.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Product 서비스가 소비하는 Kafka 메시지들을 한 클래스에 모아서 처리.
 * 메서드별로 topic/group/containerFactory, retry 정책을 각각 줄 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final ProductRepository productRepository;
    private final StockService stockService;                       // 실제 복원 로직 호출
    private final ProcessedEventRepository processedEventRepo;     // 멱등 처리

    /** 재고 복원 요청(요청/커맨드) 소비: 멱등 + 재시도 */
    @Transactional
    @RetryableTopic(
            attempts = 4,
            backoff = @Backoff(delay = 60_000, multiplier = 2.0)
    )
    @KafkaListener(
            id = "stockRestoreConsumer",
            topics = KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
            groupId = "product-stock-restore",
            containerFactory = "stockRestoreKafkaListenerContainerFactory"
    )
    public void onStockRestoreRequest(StockRestoreRequest req) {
        if (processedEventRepo.existsById(req.eventId())) return;  // 멱등
        stockService.restoreStocks(req.items());                    // 재고 복원
        processedEventRepo.save(new ProcessedEvent(req.eventId()));
    }

    /** 재고 상태 알림(Event) 소비: 읽기모델(status) 동기화 */
    @Transactional
    @KafkaListener(
            id = "stockEventsConsumer",
            topics = KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
            groupId = "product-readmodel",
            containerFactory = "stockEventsKafkaListenerContainerFactory",
            concurrency = "6"    // 파티션 수와 맞추거나 yml/팩토리로 이동해도 OK
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
            default -> { /* 무시 */ }
        }
    }
}