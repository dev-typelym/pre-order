package com.app.preorder.controller.order;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.orderDTO.OrderListDTO;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.member.Member;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import com.app.preorder.repository.member.MemberRepository;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order/*")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final MemberRepository memberRepository;
    private final OrderService orderService;

    // 단일 상품 주문
    @PostMapping("orderItem/{productId}")
    @ResponseBody
    public void orderItem(@PathVariable Long productId, @RequestParam Long count) {

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
    @PostMapping("cart")
    @ResponseBody
    public void orderItemFromCart(@RequestParam("checkedPIds[]") List<String> productIds, @RequestParam("checkedQIds[]") List<String> quantities) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        Long sessionId = null;

        if(member != null){
            sessionId = member.getId();
        }

        orderService.addOrderFromCart(sessionId, productIds, quantities);
    }

    // 주문 목록
    @GetMapping("orderList/{page}")
    @ResponseBody
    public Page<OrderListDTO> getOrderList(@PathVariable("page") int page){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        Member member = memberRepository.findByUsername(currentUsername);

        Long sessionId = null;

        if(member != null){
            sessionId = member.getId();
        }

        Page<OrderListDTO> orderList = orderService.getOrderListWithPaging(page -1, sessionId);
        return orderList;
    }
    // 주문 상세보기
    @GetMapping("detail/{orderId}")
    public String goOrderDetail(@PathVariable Long orderId, Model model){

       OrderListDTO orderInfo = orderService.getOrderItemsInOrder(orderId);
       model.addAttribute("orderInfo", orderInfo);
       return "orderList/detail";
    }

    // 주문 취소
    @PostMapping("cancel")
    @ResponseBody
    public void cancelOrder(@RequestParam Long orderId) {
        orderService.orderCancel(orderId);
    }
    // 배송중, 배송완료

    // 반품 신청
}
