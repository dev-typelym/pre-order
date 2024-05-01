package com.app.preorder.service.order;

import com.app.preorder.domain.cartDTO.CartItemListDTO;
import com.app.preorder.domain.orderDTO.OrderListDTO;
import com.app.preorder.entity.cart.CartItem;
import com.app.preorder.entity.order.Order;
import com.app.preorder.entity.order.OrderItem;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {

    // 단건 주문
    public void addOrder(Long memberId, Long productId, Long quantity);

    // 카트 다건 주문
    public void addOrderFromCart(Long memberId, List<String> productIds, List<String> quantities);

    // 주문 목록 조회
    public Page<OrderListDTO> getOrderListWithPaging(int page, Long memberId);

    // 주문 상세보기
    public OrderListDTO getOrderItemsInOrder(Long orderId);

    // 주문 취소
//    public void orderCancel(Long orderId);

    default OrderListDTO toOrderListDTO(Order order) {
        return OrderListDTO.builder()
                .id(order.getId())
                .orderDate(order.getOrderDate())
                .orderItems(order.getOrderItems())
                .orderPrice(order.getOrderPrice())
                .status(order.getStatus())
                .member(order.getMember())
                .updateDate(order.getUpdateDate())
                .build();
    }
}
