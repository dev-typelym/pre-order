package com.app.preorder.orderservice.messaging.inbox;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.type.InboxStatus;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.idempotency.OrderStepIdempotency;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.service.OrderTransactionalService;
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
public class OrderInboxProcessor {

    private final OrderInboxEventRepository repo;
    private final OrderRepository orderRepository;
    private final OrderTransactionalService tx;
    private final OrderStepIdempotency idem;
    private final ObjectMapper om;

    @Scheduled(fixedDelayString = "${inbox.order.process-interval-ms:700}")
    @Transactional
    public void flush() {
        List<OrderInboxEvent> batch = repo.findTop100ByStatusOrderByIdAsc(InboxStatus.PENDING);
        for (OrderInboxEvent e : batch) {
            try {
                StockCommandResult r = om.readValue(e.getPayloadJson(), StockCommandResult.class);

                Order order = orderRepository.findById(r.orderId()).orElse(null);
                if (order == null) { e.markProcessed(); continue; }

                // 멱등: eventId 기준 한 번만
                try { idem.begin(r.orderId(), "RESULT:" + r.eventId(), null); }
                catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
                    e.markProcessed(); continue;
                }

                StockCommandResultType t = r.result();
                switch (t) {
                    case COMMITTED -> {
                        tx.completeOrder(order);
                        // 기타 후처리 필요 시 추가
                    }
                    case RESERVE_FAILED -> {
                        // 필요 시 주문 준비 실패 상태 전환 메서드 연결(없으면 로그만)
                        // tx.markPrepareFailed(order, r.reason());
                        log.warn("[Inbox][order] 예약 실패 처리: orderId={}, reason={}", r.orderId(), r.reason());
                    }
                    case COMMIT_FAILED -> {
                        // 필요 시 결제 실패/롤백 상태 전환 메서드 연결(없으면 로그만)
                        // tx.markPaymentFailed(order, r.reason());
                        log.warn("[Inbox][order] 커밋 실패 처리: orderId={}, reason={}", r.orderId(), r.reason());
                    }
                    case RESERVED, UNRESERVED, RESTORED -> {
                        // 선택적 후처리
                        log.info("[Inbox][order] 후처리 이벤트: orderId={}, result={}", r.orderId(), t);
                    }
                    default -> { /* no-op */ }
                }
                e.markProcessed();
            } catch (Exception ex) {
                log.warn("[Inbox][order] 처리 실패 id={}, topic={}, 사유={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
