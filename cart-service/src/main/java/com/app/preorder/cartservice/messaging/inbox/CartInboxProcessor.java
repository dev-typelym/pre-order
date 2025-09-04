package com.app.preorder.cartservice.messaging.inbox;

import com.app.preorder.cartservice.service.cart.CartService;
import com.app.preorder.common.messaging.command.CartCreateRequest;
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
    public void processPending() {
        List<CartInboxEvent> batch = repo.findTop100ByStatusOrderByIdAsc(InboxStatus.PENDING);
        for (CartInboxEvent e : batch) {
            try {
                CartCreateRequest cmd = om.readValue(e.getPayloadJson(), CartCreateRequest.class);
                // 멱등 보장: "있으면 유지, 없으면 생성"
                cartService.ensureCartExists(cmd.memberId());
                e.markProcessed();
            } catch (Exception ex) {
                log.warn("[Inbox][cart] 처리 실패 id={}, reason={}", e.getId(), ex.toString());
                e.markFailed(ex.toString());
            }
        }
    }
}
