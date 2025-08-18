package com.app.preorder.productservice.messaging.consumer;

import com.app.preorder.common.events.StockEvent;
import com.app.preorder.common.events.Topics;
import com.app.preorder.common.type.ProductStatus;
import com.app.preorder.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class StockEventsConsumer {

    private final ProductRepository productRepository;

    @Transactional
    @KafkaListener(
            topics = Topics.INVENTORY_STOCK_EVENTS_V1,
            groupId = "product-readmodel",
            concurrency = "6" // 파티션 수(예: 6) 이하면 OK. yml로 뺄 수도 있음.
    )
    public void on(StockEvent evt) {
        if ("SOLD_OUT".equals(evt.type())) {
            productRepository.updateStatus(evt.productId(), ProductStatus.SOLD_OUT);
            return;
        }
        if ("STOCK_CHANGED".equals(evt.type())) {
            if (evt.available() != null && evt.available() > 0) {
                productRepository.updateStatus(evt.productId(), ProductStatus.ENABLED);
            } else {
                productRepository.updateStatus(evt.productId(), ProductStatus.SOLD_OUT);
            }
        }
    }
}
