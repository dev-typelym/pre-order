package com.app.preorder.productservice.messaging.publisher;

import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class KafkaProductEventPublisher implements ProductEventPublisher {

    private final KafkaTemplate<String, StockEvent> kafka; // ✅ 값 타입 = StockEvent

    private void publish(String type, long productId, Long available) {
        var evt = new StockEvent(type, productId, available, Instant.now().toString());
        // 파티셔닝 키 = productId → 동일 상품 순서 보장
        kafka.send(KafkaTopics.INVENTORY_STOCK_EVENTS_V1, String.valueOf(productId), evt);
    }

    @Override
    public void publishStockChanged(long productId, long availableAfter) {
        publish("STOCK_CHANGED", productId, availableAfter);
    }

    @Override
    public void publishSoldOut(long productId) {
        publish("SOLD_OUT", productId, 0L);
    }
}
