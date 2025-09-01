// product-service/src/main/java/com/app/preorder/productservice/messaging/inbox/ProductInboxProcessor.java
package com.app.preorder.productservice.messaging.inbox;

import com.app.preorder.common.messaging.command.StockRestoreRequest;
import com.app.preorder.common.type.InboxStatus;
import com.app.preorder.productservice.service.StockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductInboxProcessor {

    private final ProductInboxEventRepository inboxRepo;
    private final StockService stockService;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${inbox.product.process-interval-ms:700}")
    @Transactional
    public void flush() {
        List<ProductInboxEvent> batch = inboxRepo.findTop100ByStatusOrderByIdAsc(InboxStatus.PENDING);
        for (ProductInboxEvent e : batch) {
            try {
                StockRestoreRequest req = om.readValue(e.getPayloadJson(), StockRestoreRequest.class);
                stockService.restoreStocks(req.items());
                e.markProcessed();
            } catch (Exception ex) {
                log.warn("[Inbox][product] 처리 실패 id={}, topic={}, reason={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
