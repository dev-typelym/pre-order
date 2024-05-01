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
    public Long addOrder(Long memberId, Long productId, Long quantity);

    // 카트 다건 주문
    public Long addOrderFromCart(Long memberId, List<String> productIds, List<String> quantities);

    // 주문 목록 조회
    public Page<OrderListDTO> getOrderListWithPaging(int page, Long memberId);

    // 주문 상세보기
    public OrderListDTO getOrderItemsInOrder(Long orderId);

    // 주문 취소
    public void orderCancel(Long orderId);

    // 반품 신청
    public void orderReturn(Long orderId);

    // 주문상태 배달중으로 바꾸는 스케쥴링 메소드
    public void scheduleOrderShipping(Long orderId);

    // 주문상태 배달중에서 배달완료로 바꾸는 스케쥴링 메소드
    public void scheduleOrderDelivered(Long orderId);

    // 반품 신청후 처리를 스케줄링 하는 메소드
    public void scheduleReturnProcess(Long orderId);

    // 배송완료후 1일 이후엔 배송상태를 반품 불가로 바꾸는 메소드
    public void scheduleNonReturnable(Long orderId);

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
