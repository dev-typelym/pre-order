package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.cartservice.service.cart.CartService;
import com.app.preorder.common.messaging.command.CartCreateRequest;
import com.app.preorder.common.messaging.event.MemberDeactivatedEvent;
import com.app.preorder.common.messaging.event.OrderCompletedEvent;
import com.app.preorder.common.messaging.event.StockEvent;
import com.app.preorder.common.messaging.event.ProductStatusChangedEvent;
import com.app.preorder.common.messaging.topics.KafkaTopics;
import com.app.preorder.common.type.InboxStatus;
import com.app.preorder.common.type.ProductStatus;
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
                        MemberDeactivatedEvent evt = om.readValue(e.getPayloadJson(), MemberDeactivatedEvent.class);
                        cartService.deleteCart(evt.memberId());
                    }
                    case KafkaTopics.INVENTORY_STOCK_EVENTS_V1 -> {
                        StockEvent evt = om.readValue(e.getPayloadJson(), StockEvent.class);
                        boolean soldOut = "SOLD_OUT".equalsIgnoreCase(evt.type());
                        boolean noneAvail = evt.available() != null && evt.available() == 0L;
                        if (soldOut || noneAvail) {
                            cartService.deleteItemsByProductIds(List.of(evt.productId()));
                        } else {
                            log.debug("[Inbox][cart] STOCK_EVENTS ignored (type={}, available={}) pid={}",
                                    evt.type(), evt.available(), evt.productId());
                        }
                    }
                    case KafkaTopics.PRODUCT_STATUS_CHANGED_V1 -> {
                        ProductStatusChangedEvent evt = om.readValue(e.getPayloadJson(), ProductStatusChangedEvent.class);
                        if (evt.status() == ProductStatus.DISABLED) {
                            cartService.deleteItemsByProductIds(List.of(evt.productId()));
                        } else {
                            log.debug("[Inbox][cart] PRODUCT_STATUS_CHANGED ignored (status={}) pid={}",
                                    evt.status(), evt.productId());
                        }
                    }
                    case KafkaTopics.ORDER_COMPLETED_V1 -> {
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
