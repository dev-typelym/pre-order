package com.app.preorder.orderservice.messaging.publisher;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, StockRestoreRequest> stockRestoreKafkaTemplate;

    @Override
    public void publishStockRestoreRequest(Long orderId, List<StockRequestInternal> items) {
        // 파티션 키: 보수적으로 productId(첫 아이템) 또는 orderId
        Long partitionKey = items.isEmpty() ? orderId : items.get(0).getProductId();

        StockRestoreRequest payload = new StockRestoreRequest(
                orderId,
                items,
                UUID.randomUUID().toString(),   // 멱등키
                partitionKey
        );

        Runnable publish = () ->
                stockRestoreKafkaTemplate.send(
                        KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1,
                        String.valueOf(partitionKey),
                        payload
                );

        // 트랜잭션이 열려있으면 커밋 이후에 발행(이중처리/롤백 시 발행 방지)
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() { publish.run(); }
            });
        } else {
            publish.run();
        }
    }
}

