package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.messaging.command.*;
import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.productservice.messaging.publisher.ProductEventPublisher;
import com.app.preorder.productservice.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.app.preorder.common.type.StockCommandResultType.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInboxProcessor {

    private final ProductInboxEventRepository inboxRepo;
    private final StockService stockService;
    private final ProductEventPublisher resultPublisher;
    private final ObjectMapper om;

    @Value("${inbox.product.process-batch-size:1000}")
    private int batchSize;

    /**
     * 1) PENDING 행을 FOR UPDATE로 잠가 선점
     * 2) 잠근 id들만 로드해서 처리
     * 3) 성공: markProcessed(), 실패: markFailed(reason)
     */
    @Scheduled(fixedDelayString = "${inbox.product.process-interval-ms:200}")
    @Transactional(noRollbackFor = Exception.class)
    public void flush() {
        // 1) 선점(락)
        List<Long> ids = inboxRepo.lockPendingIds(batchSize);
        if (ids.isEmpty()) return;

        // 2) 처리 대상 로드
        List<ProductInboxEvent> batch = inboxRepo.findByIdInOrderByIdAsc(ids);

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
                    default -> throw new IllegalArgumentException("지원되지 않는 토픽: " + e.getTopic());
                }
                e.markProcessed();                // ★ 여기
            } catch (Exception ex) {
                log.warn("[Inbox][product] 처리 실패 id={}, topic={}, 이유={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());      // ★ 여기
            }
        }
    }

    private void publishResult(String eventId, Long orderId, StockCommandResultType type, String reason) {
        resultPublisher.publishStockCommandResult(
                new StockCommandResult(eventId, orderId, type, reason, Instant.now().toString())
        );
    }
}
