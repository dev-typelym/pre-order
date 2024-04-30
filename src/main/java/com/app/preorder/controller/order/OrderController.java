package com.app.preorder.controller.order;

import com.app.preorder.entity.member.Member;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order/*")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final MemberRepository memberRepository;
    private final OrderService orderService;

    // 단일 상품 주문
    // 카트에 아이템 추가
    @PostMapping("orderItem/add/{productId}")
    @ResponseBody
    public void addItemToCart(@PathVariable Long productId, @RequestParam Long count) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        Long sessionId = null;

        if(member != null){
            sessionId = member.getId();
        }

        orderService.addOrder(sessionId, productId, count);
    }

    // 카트에서 주문
}
