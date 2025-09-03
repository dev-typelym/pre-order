package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.messaging.command.*;
import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.InboxStatus;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.productservice.messaging.publisher.ProductEventPublisher;
import com.app.preorder.productservice.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.app.preorder.common.type.StockCommandResultType.*; // ★ enum 상수 static import

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInboxProcessor {

    private final ProductInboxEventRepository inboxRepo;
    private final StockService stockService;
    private final ProductEventPublisher resultPublisher;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${inbox.product.process-interval-ms:700}")
    @Transactional
    public void flush() {
        List<ProductInboxEvent> batch =
                inboxRepo.findTop100ByStatusOrderByIdAsc(InboxStatus.PENDING);

        for (ProductInboxEvent e : batch) {
            try {
                switch (e.getTopic()) {
                    case KafkaTopics.INVENTORY_STOCK_RESERVE_REQUEST_V1 -> {
                        var req = om.readValue(e.getPayloadJson(), ReserveStocksRequest.class);
                        try {
                            stockService.reserveStocks(req.items());
                            publishResult(req.eventId(), req.orderId(), RESERVED, null);
                        } catch (Exception op) {
                            publishResult(req.eventId(), req.orderId(), RESERVE_FAILED, op.getMessage());
                            throw op;
                        }
                    }
                    case KafkaTopics.INVENTORY_STOCK_COMMIT_REQUEST_V1 -> {
                        var req = om.readValue(e.getPayloadJson(), CommitStocksRequest.class);
                        try {
                            stockService.commitStocks(req.items());
                            publishResult(req.eventId(), req.orderId(), COMMITTED, null);
                        } catch (Exception op) {
                            publishResult(req.eventId(), req.orderId(), COMMIT_FAILED, op.getMessage());
                            throw op;
                        }
                    }
                    case KafkaTopics.INVENTORY_STOCK_UNRESERVE_REQUEST_V1 -> {
                        var req = om.readValue(e.getPayloadJson(), UnreserveStocksRequest.class);
                        stockService.unreserveStocks(req.items());
                        publishResult(req.eventId(), req.orderId(), UNRESERVED, null);
                    }
                    case KafkaTopics.INVENTORY_STOCK_RESTORE_REQUEST_V1 -> {
                        var req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                        stockService.restoreStocks(req.items());
                        publishResult(req.eventId(), req.orderId(), RESTORED, null);
                    }
                    default -> throw new IllegalArgumentException("지원되지 않는 토픽입니다: " + e.getTopic());
                }
                e.markProcessed();
            } catch (Exception ex) {
                log.warn("[Inbox][product] 처리 실패 id={}, topic={}, 사유={}",
                        e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }

    private void publishResult(String eventId, Long orderId, StockCommandResultType type, String reason) {
        resultPublisher.publishStockCommandResult(
                new StockCommandResult(eventId, orderId, type, reason, Instant.now().toString())
        );
    }
}
