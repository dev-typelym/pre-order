package com.app.preorder.orderservice.messaging.consumer;

import com.app.preorder.common.dto.StockRequestInternal;
import com.app.preorder.common.messaging.event.StockCommandResult;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.StockCommandResultType;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.idempotency.OrderStepIdempotency;
import com.app.preorder.orderservice.messaging.publisher.OrderCommandPublisher; // ✅ 추가
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

    private final OrderCommandPublisher commandPublisher; // ✅ 추가

    // ========= 1) 재고 커맨드 결과 =========
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
                    tx.completeOrder(order);
                    log.info("[OrderResult] 결제 커밋 완료 처리: orderId={}", r.orderId());
                }

                case COMMIT_FAILED -> {
                    // ✅ 보상: 예약 해제 커맨드 발행
                    var items = order.getOrderItems().stream()
                            .map(i -> new StockRequestInternal(i.getProductId(), i.getProductQuantity()))
                            .toList();

                    commandPublisher.publishUnreserveCommand(order.getId(), items); // Outbox → Kafka
                    tx.cancelOrderInTransaction(order); // 정책에 맞게 실패/취소 상태 전이

                    log.warn("[OrderResult] 재고 커밋 실패  →  UNRESERVE 발행: orderId={}, reason={}", r.orderId(), r.reason());
                }

                case RESERVE_FAILED -> {
                    // 예약 자체가 실패했으면 주문을 취소로 정리(원하면 유지/변경)
                    tx.cancelOrderInTransaction(order);
                    log.warn("[OrderResult] 재고 예약 실패: orderId={}, reason={}", r.orderId(), r.reason());
                }

                case RESERVED, UNRESERVED, RESTORED -> {
                    log.info("[OrderResult] 후처리 이벤트 수신: orderId={}, result={}", r.orderId(), t);
                }

                default -> log.info("[OrderResult] 처리 대상 아님: orderId={}, result={}", r.orderId(), t);
            }
        } catch (RuntimeException e) {
            idem.undo(r.orderId(), "RESULT:" + r.eventId(), null);
            throw e;
        }
    }

    // (다른 리스너는 그대로)
}
