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

    private final ProductRepository productRepository;

    @Transactional
    @KafkaListener(
            id = "stockEventsConsumer",
            topics = KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
            groupId = "product-readmodel",
            containerFactory = "stockEventsKafkaListenerContainerFactory"
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
