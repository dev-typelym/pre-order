package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.cartservice.service.cart.CartService;
import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.event.MemberDeactivatedEvent; // ✅ 추가
import com.app.preorder.common.messaging.event.OrderCompletedEvent;   // ✅ 추가
import com.app.preorder.common.messaging.event.StockEvent;            // ✅ 추가
import com.app.preorder.common.messaging.topics.KafkaTopics;          // ✅ 추가
import com.app.preorder.common.type.InboxStatus;
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
public class CartInboxProcessor {

    private final CartInboxEventRepository repo;
    private final ObjectMapper om;
    private final CartService cartService;

    @Scheduled(fixedDelay = 1000, initialDelay = 2000)
    @Transactional
    public void flush() {
        List<CartInboxEvent> batch = repo.findTop100ByStatusOrderByIdAsc(InboxStatus.PENDING);
        for (CartInboxEvent e : batch) {
            try {
                switch (e.getTopic()) {
                    case KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1 -> {
                        // 기존: 카트 생성 보장
                        CartCreateRequest cmd = om.readValue(e.getPayloadJson(), CartCreateRequest.class);
                        cartService.ensureCartExists(cmd.memberId());
                    }
                    case KafkaTopics.MEMBER_DEACTIVATED_V1 -> {
                        // ✅ 멤버 탈퇴 → 카트 전체 삭제
                        MemberDeactivatedEvent evt = om.readValue(e.getPayloadJson(), MemberDeactivatedEvent.class);
                        cartService.deleteCart(evt.memberId());
                    }
                    case KafkaTopics.INVENTORY_STOCK_EVENTS_V1 -> {
                        // ✅ 상품 SOLD_OUT/가용 0 → 전체 카트에서 해당 상품 정리
                        StockEvent evt = om.readValue(e.getPayloadJson(), StockEvent.class);
                        cartService.deleteItemsByProductIds(List.of(evt.productId()));
                    }
                    case KafkaTopics.ORDER_COMPLETED_V1 -> {
                        // ✅ 카트로 결제 완료 → 해당 멤버의 해당 상품들만 카트에서 제거
                        //    (BUY_NOW는 건드리지 않음)
                        OrderCompletedEvent evt = om.readValue(e.getPayloadJson(), OrderCompletedEvent.class);
                        if ("CART".equalsIgnoreCase(evt.orderType())) {
                            cartService.deleteItemsByMemberAndProducts(evt.memberId(), evt.productIds());
                        } else {
                            log.debug("[Inbox][cart] ORDER_COMPLETED_V1 ignored (orderType={} != CART), memberId={}",
                                    evt.orderType(), evt.memberId());
                        }
                    }
                    default -> {
                        log.debug("[Inbox][cart] Unknown topic {}, mark processed id={}", e.getTopic(), e.getId());
                        // 알 수 없는 토픽은 큐 정체 방지를 위해 소거
                    }
                }
                e.markProcessed();
            } catch (Exception ex) {
                log.warn("[Inbox][cart] 처리 실패 id={}, topic={}, reason={}", e.getId(), e.getTopic(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
