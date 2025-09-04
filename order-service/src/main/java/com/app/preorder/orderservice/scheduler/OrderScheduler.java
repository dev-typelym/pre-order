package com.app.preorder.orderservice.scheduler;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.orderservice.idempotency.OrderStepIdempotency;
import com.app.preorder.orderservice.messaging.publisher.OrderCommandPublisher;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.service.OrderTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component("orderExpirationSweepScheduler")
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository repo;
    private final OrderCommandPublisher publisher;
    private final OrderTransactionalService tx;
    private final OrderStepIdempotency idem;

    @Value("${order.expiration.batch-size:300}")
    private int batchSize;

    @Value("${order.expiration.grace-seconds:10}")
    private int graceSeconds;

    @Value("${order.expiration.max-rounds:10}") // 한 틱에 최대 라운드
    private int maxRounds;

    @Scheduled(fixedDelayString = "${order.expiration.sweep-interval-ms:10000}")
    @Transactional
    public void sweepExpiredBeforeCommit() {
        var threshold = LocalDateTime.now().minusSeconds(graceSeconds);

        int rounds = 0;
        while (rounds++ < maxRounds) {
            var list = repo.findExpiredBeforeCommit(threshold, batchSize);
            if (list.isEmpty()) break;

            for (var o : list) {
                try { idem.begin(o.getId(), "EXPIRE:" + o.getId(), null); }
                catch (OrderStepIdempotency.AlreadyProcessedException ignore) { continue; }

                var items = o.getOrderItems().stream()
                        .map(i -> new StockRequestInternal(
                                i.getProductId(),
                                i.getProductQuantity()    // 필요 시 .intValue()
                        ))
                        .toList();

                publisher.publishUnreserveCommand(o.getId(), items);
                tx.cancelOrderInTransaction(o); // 내부에서 expiresAt=null 처리 권장
                log.info("[SWEEP] expired→UNRESERVE: orderId={}", o.getId());
            }

            if (list.size() < batchSize) break; // 이번 라운드로 충분히 비웠으면 종료
        }
    }
}
