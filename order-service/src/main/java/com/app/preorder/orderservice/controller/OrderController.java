package com.app.preorder.orderservice.controller;

import com.app.preorder.common.dto.ApiResponse;
import com.app.preorder.common.dto.TokenPayload;
import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    // 단일 상품 주문
    @PostMapping("/items/{productId}")
    public ResponseEntity<ApiResponse<Long>> orderItem(
            @PathVariable Long productId,
            @RequestParam Long count) {

        TokenPayload payload = (TokenPayload) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long memberId = payload.getId();

        Long orderId = orderService.orderSingleItem(memberId, productId, count);
        return ResponseEntity.ok(ApiResponse.success(orderId, "주문이 완료되었습니다."));
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

        Long orderId = orderService.addOrderFromCart(sessionId, productIds, quantities);
        orderService.scheduleOrderShipping(orderId);
        orderService.scheduleOrderDelivered(orderId);
        orderService.scheduleNonReturnable(orderId);
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


    // 반품 신청
    @PostMapping("return")
    @ResponseBody
    public void returnOrder(@RequestParam Long orderId) {
        orderService.orderReturn(orderId);
        orderService.scheduleReturnProcess(orderId);
    }
}
