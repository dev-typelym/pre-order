package com.app.preorder.orderservice.controller;

import com.app.preorder.common.dto.PendingQuantityInternal;
import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderInternalController {

    private final OrderService orderService;

    /** [Feign] 특정 상품의 결제 대기 수량 조회 */
    @PostMapping("/pending-quantities")
    public List<PendingQuantityInternal> getPendingQuantities(@RequestBody List<Long> productIds) {
        return orderService.getPendingQuantities(productIds);
    }
}
