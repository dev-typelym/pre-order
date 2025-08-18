package com.app.preorder.productservice.messaging.producer;

import com.app.preorder.common.events.StockEvent;
import com.app.preorder.common.events.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class KafkaStockEventProducer implements StockEventProducer {

    private final KafkaTemplate<String, StockEvent> kafka; // ✅ 값 타입 = StockEvent

    private void send(String type, long productId, Long available) {
        var evt = new StockEvent(type, productId, available, Instant.now().toString());
        // 파티셔닝 키 = productId → 동일 상품 순서 보장
        kafka.send(Topics.INVENTORY_STOCK_EVENTS_V1, String.valueOf(productId), evt);
    }

    @Override
    public void sendStockChanged(long productId, long availableAfter) {
        send("STOCK_CHANGED", productId, availableAfter);
    }

    @Override
    public void sendSoldOut(long productId) {
        send("SOLD_OUT", productId, 0L);
    }
}
