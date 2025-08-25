package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.productservice.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInboxProcessor {

    private final ProductInboxEventRepository inboxRepo;
    private final StockService stockService;
    private final ObjectMapper om;
    private final TransactionTemplate tx; // 스프링이 주입 (PlatformTxManager 필요)

    // 0.7초마다 최대 100건 처리
    @Scheduled(fixedDelay = 700)
    public void flush() {
        List<ProductInboxEvent> batch =
                inboxRepo.findTop100ByStatusOrderByCreatedAtAsc(ProductInboxStatus.PENDING);
        if (batch.isEmpty()) return;

        for (ProductInboxEvent e : batch) {
            tx.executeWithoutResult(st -> {
                try {
                    var req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                    stockService.restoreStocks(req.items()); // 비즈 처리
                    e.markProcessed();                       // PENDING -> PROCESSED
                    inboxRepo.save(e);
                } catch (Exception ex) {
                    // 실패는 그대로 PENDING 유지 → 다음 주기에 재시도
                    log.warn("Inbox process failed: id={}, reason={}", e.getId(), ex.toString());
                }
            });
        }
    }
}
