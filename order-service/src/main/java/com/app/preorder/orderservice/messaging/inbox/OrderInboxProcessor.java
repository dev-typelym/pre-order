package com.app.preorder.orderservice.messaging.inbox;

import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.type.InboxStatus;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.common.type.OrderStatus;
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

                // 멱등: result 이벤트(eventId)당 한 번만 처리
                try { idem.begin(r.orderId(), "RESULT:" + r.eventId(), null); }
                catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
                    e.markProcessed(); continue;
                }

                StockCommandResultType t = r.result();
                switch (t) {
                    case COMMITTED -> {
                        // ✅ 결제 진행 중일 때만 완료 전이
                        if (order.getStatus() == OrderStatus.PAYMENT_PROCESSING) {
                            tx.completeOrder(order);
                            log.info("[Inbox][order] COMMITTED → ORDER_COMPLETE: orderId={}", r.orderId());
                        } else {
                            log.warn("[Inbox][order] COMMITTED 무시: orderId={}, status={}", r.orderId(), order.getStatus());
                        }
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

                    case UNRESERVED -> {
                        // ✅ 결제 전 단계였다면 '만료 취소'(expiresAt 유지)
                        switch (order.getStatus()) {
                            case PAYMENT_PREPARING, PAYMENT_PROCESSING -> {
                                tx.cancelOrderByExpiry(order); // expiresAt 유지
                                log.info("[Inbox][order] UNRESERVED → ORDER_CANCEL(by expiry): orderId={}", r.orderId());
                            }
                            case ORDER_CANCEL, ORDER_COMPLETE -> {
                                // 이미 닫힌 주문이면 멱등 no-op
                                log.debug("[Inbox][order] UNRESERVED no-op (already closed): orderId={}, status={}", r.orderId(), order.getStatus());
                            }
                            default -> {
                                log.warn("[Inbox][order] UNRESERVED in unexpected status: orderId={}, status={}", r.orderId(), order.getStatus());
                            }
                        }
                    }

                    case RESERVED, RESTORED -> {
                        // 선택적 후처리만(상태 변경 없음)
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
