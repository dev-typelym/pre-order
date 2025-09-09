package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.event.MemberDeactivatedEvent; // ✅ 추가
import com.app.preorder.common.messaging.event.StockEvent;            // ✅ 추가
import com.app.preorder.common.messaging.event.OrderCompletedEvent;   // ✅ 추가
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartInboxWriter {

    private final CartInboxEventRepository repo;
    private final ObjectMapper om;

    /** 기존: 카트 생성 요청 인박스 적재 */
    @Transactional
    @KafkaListener(
            id = "cart-inbox-writer-cart-create",
            topics = KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1,
            groupId = "cart-inbox-writer",
            containerFactory = "cartCreateListenerContainerFactory"
    )
    public void onCartCreate(CartCreateRequest cmd) {
        try {
            if (repo.existsByMessageKey(cmd.eventId())) return;
            String json = om.writeValueAsString(cmd);
            repo.save(CartInboxEvent.of(
                    cmd.eventId(),
                    KafkaTopics.MEMBER_CART_CREATE_REQUEST_V1,
                    json
            ));
        } catch (Exception e) {
            throw new RuntimeException("Inbox write failed (cart-create)", e);
        }
    }

    /** ✅ 추가: 멤버 탈퇴 이벤트 인박스 적재 */
    @Transactional
    @KafkaListener(
            id = "cart-inbox-writer-member-deactivated",
            topics = KafkaTopics.MEMBER_DEACTIVATED_V1,
            groupId = "cart-inbox-writer",
            containerFactory = "memberDeactivatedKafkaListenerContainerFactory"
    )
    public void onMemberDeactivated(MemberDeactivatedEvent evt) {
        try {
            if (repo.existsByMessageKey(evt.eventId())) return;
            String json = om.writeValueAsString(evt);
            repo.save(CartInboxEvent.of(
                    evt.eventId(),
                    KafkaTopics.MEMBER_DEACTIVATED_V1,
                    json
            ));
        } catch (Exception e) {
            throw new RuntimeException("Inbox write failed (member-deactivated)", e);
        }
    }

    /** ✅ 추가: 재고 이벤트(SOLD_OUT/available==0만) 인박스 적재 */
    @Transactional
    @KafkaListener(
            id = "cart-inbox-writer-stock-events",
            topics = KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
            groupId = "cart-inbox-writer",
            containerFactory = "stockEventsKafkaListenerContainerFactory"
    )
    public void onStockEvent(StockEvent evt) {
        try {
            // 노이즈 컷: 품절/가용 0만 적재
            boolean soldOut = "SOLD_OUT".equalsIgnoreCase(evt.type());
            boolean noneAvail = evt.available() != null && evt.available() == 0L;
            if (!(soldOut || noneAvail)) return;

            if (repo.existsByMessageKey(evt.eventId())) return;
            String json = om.writeValueAsString(evt);
            repo.save(CartInboxEvent.of(
                    evt.eventId(),
                    KafkaTopics.INVENTORY_STOCK_EVENTS_V1,
                    json
            ));
        } catch (Exception e) {
            throw new RuntimeException("Inbox write failed (stock-events)", e);
        }
    }

    /** ✅ 추가: 주문 완료 이벤트 인박스 적재 (BUY_NOW 필터링은 Processor에서) */
    @Transactional
    @KafkaListener(
            id = "cart-inbox-writer-order-completed",
            topics = KafkaTopics.ORDER_COMPLETED_V1,
            groupId = "cart-inbox-writer",
            containerFactory = "orderCompletedKafkaListenerContainerFactory"
    )
    public void onOrderCompleted(OrderCompletedEvent evt) {
        try {
            if (repo.existsByMessageKey(evt.eventId())) return;
            String json = om.writeValueAsString(evt);
            repo.save(CartInboxEvent.of(
                    evt.eventId(),
                    KafkaTopics.ORDER_COMPLETED_V1,
                    json
            ));
        } catch (Exception e) {
            throw new RuntimeException("Inbox write failed (order-completed)", e);
        }
    }
}
