package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.OrderScheduleFailedException;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import com.app.preorder.orderservice.domain.vo.OrderAddress;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderTransactionalService {

    private final OrderRepository orderRepository;
    private final OrderScheduler orderScheduler;
    private final OrderFactory orderFactory;

    /** 단건 주문 생성(prepare 단계): 예약 성공 후 주문 저장 */
    @Transactional
    public Long saveOrderInTransaction(Long memberId, ProductInternal product, Long quantity) {
        Order order = orderFactory.createOrder(memberId, product, quantity);
        orderRepository.save(order);
        return order.getId();
    }

    /** 장바구니 주문 생성(prepare 단계): 전체 품목 예약 성공 후 주문 저장 */
    @Transactional
    public Long saveOrderFromCartInTransaction(Long memberId, List<ProductInternal> products, Map<Long, Long> quantityMap) {
        Order order = orderFactory.createOrderFromCart(memberId, products, quantityMap);
        orderRepository.save(order);
        return order.getId();
    }

    /** 결제 시도(attempt 단계): 상태 → PAYMENT_PROCESSING (DB만 갱신) */
    @Transactional
    public void updateOrderStatusToProcessing(Order order) {
        order.updateOrderStatus(OrderStatus.PAYMENT_PROCESSING);
        orderRepository.save(order);
    }

    /**
     * 결제 완료(complete 단계): 상태 → ORDER_COMPLETE
     * 스케줄 등록은 트랜잭션 커밋 이후(afterCommit)로 밀어 데이터/스케줄 불일치 방지
     */
    @Transactional
    public void completeOrder(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_COMPLETE);
        orderRepository.save(order);

        final Long orderId = order.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    orderScheduler.scheduleAll(orderId);
                } catch (Exception e) {
                    // 커밋 이후라 DB 롤백은 없고, 실패는 모니터링/알람으로 처리
                    throw new OrderScheduleFailedException("주문 스케줄 등록 실패", e);
                }
            }
        });
    }

    /** 배송지 변경: 주문의 배송 주소 갱신 */
    @Transactional
    public void updateOrderAddressInTransaction(Order order, UpdateOrderAddressRequest request) {
        OrderAddress newAddress = new OrderAddress(
                request.getZipCode(),
                request.getStreetAddress(),
                request.getDetailAddress()
        );
        order.updateDeliveryAddress(newAddress);
        orderRepository.save(order); // ← 저장 누락 보완
    }

    /** 주문 취소: 상태 → ORDER_CANCEL */
    @Transactional
    public void cancelOrderInTransaction(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_CANCEL);
        orderRepository.save(order);
    }

    /** 반품 진행: 상태 → RETURNING */
    @Transactional
    public void updateOrderStatusToReturning(Order order) {
        order.updateOrderStatus(OrderStatus.RETURNING);
        orderRepository.save(order);
    }

    /** 반품 완료: 상태 → RETURN_COMPLETE */
    @Transactional
    public void updateOrderStatusToReturnComplete(Order order) {
        order.updateOrderStatus(OrderStatus.RETURN_COMPLETE);
        orderRepository.save(order);
    }

    /** 배송 중: 상태 → DELIVERYING */
    @Transactional
    public void updateOrderStatusToShipping(Order order) {
        order.updateOrderStatus(OrderStatus.DELIVERYING);
        orderRepository.save(order);
    }

    /** 배송 완료: 상태 → DELIVERY_COMPLETE */
    @Transactional
    public void updateOrderStatusToDelivered(Order order) {
        order.updateOrderStatus(OrderStatus.DELIVERY_COMPLETE);
        orderRepository.save(order);
    }

    /** 반품 불가: 상태 → RETURN_NOT_PERMITTED */
    @Transactional
    public void updateOrderStatusToNonReturnable(Order order) {
        order.updateOrderStatus(OrderStatus.RETURN_NOT_PERMITTED);
        orderRepository.save(order);
    }
}
