package com.app.preorder.orderservice.messaging.consumer;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.idempotency.OrderStepIdempotency;
import com.app.preorder.orderservice.messaging.publisher.OrderCommandPublisher;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.service.OrderTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final OrderTransactionalService tx;
    private final OrderStepIdempotency idem;
    private final OrderCommandPublisher commandPublisher;

    /**
     * 재고 커맨드 결과 수신
     */
    @KafkaListener(
            id = "order-stock-result-consumer",
            topics = KafkaTopics.INVENTORY_STOCK_COMMAND_RESULTS_V1,
            groupId = "order-service",
            containerFactory = "stockCommandResultKafkaListenerContainerFactory"
    )
    @Transactional
    public void onStockCommandResult(StockCommandResult r) {
        Order order = orderRepository.findById(r.orderId()).orElse(null);
        if (order == null) {
            log.warn("[OrderResult] 주문을 찾을 수 없습니다. orderId={}", r.orderId());
            return;
        }

        // 멱등 처리: 동일 eventId는 한 번만
        try {
            idem.begin(r.orderId(), "RESULT:" + r.eventId(), null);
        } catch (OrderStepIdempotency.AlreadyProcessedException ignore) {
            return;
        }

        try {
            StockCommandResultType t = r.result();
            switch (t) {
                case COMMITTED -> {
                    // 재고 커밋 성공 → 주문 완료
                    tx.completeOrder(order);
                    log.info("[OrderResult] 결제 커밋 완료 처리: orderId={}", r.orderId());
                }

                case COMMIT_FAILED -> {
                    // 커밋 실패 → 보상: 예약 해제 명령 발행 + 주문 취소
                    var items = order.getOrderItems().stream()
                            .map(i -> new StockRequestInternal(i.getProductId(), i.getProductQuantity()))
                            .toList();
                    commandPublisher.publishUnreserveCommand(order.getId(), items); // Outbox → Kafka
                    tx.cancelOrderInTransaction(order);
                    log.warn("[OrderResult] 재고 커밋 실패 → UNRESERVE 발행 및 주문 취소: orderId={}, reason={}", r.orderId(), r.reason());
                }

                case RESERVED -> {
                    // 예약 확정 수신 → 예약 확인 전용 상태(PROCESSING)로 전이
                    if (order.getStatus() == OrderStatus.PAYMENT_PREPARING) {
                        order.updateOrderStatus(OrderStatus.PAYMENT_PROCESSING); // 트랜잭션 내 더티체킹
                        log.info("[OrderResult] RESERVED 수신 → PROCESSING 전이: orderId={}", r.orderId());
                    } else {
                        log.info("[OrderResult] RESERVED 수신(상태 유지): orderId={}, current={}", r.orderId(), order.getStatus());
                    }
                }

                case RESERVE_FAILED -> {
                    // 예약 실패 → 주문 취소(필요 시 정책 조정)
                    tx.cancelOrderInTransaction(order);
                    log.warn("[OrderResult] 재고 예약 실패: orderId={}, reason={}", r.orderId(), r.reason());
                }

                case UNRESERVED, RESTORED -> {
                    // 후처리 알림(로그만)
                    log.info("[OrderResult] 후처리 이벤트 수신: orderId={}, result={}", r.orderId(), t);
                }

                default -> {
                    log.info("[OrderResult] 처리 대상 아님: orderId={}, result={}", r.orderId(), t);
                }
            }
        } catch (RuntimeException e) {
            idem.undo(r.orderId(), "RESULT:" + r.eventId(), null);
            throw e;
        }
    }
}
