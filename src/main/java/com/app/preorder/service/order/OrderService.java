package com.app.preorder.service.order;

public interface OrderService {

    // 단건 주문
    public void addOrder(Long memberId, Long productId, Long quantity);

    // 카트 다건 주문
}
