package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.OrderScheduleFailedException;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import com.app.preorder.orderservice.domain.vo.OrderAddress;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderQuartzScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
@Service
public class OrderTransactionalService {

    private final OrderRepository orderRepository;
    private final OrderQuartzScheduler orderQuartzScheduler;
    private final OrderFactory orderFactory;

    // 홀드 타임(3분) + 지터
    private static final long HOLD_MINUTES = 3L;
    private static final Random RND = new Random();
    private static LocalDateTime holdUntil() {
        long base = HOLD_MINUTES * 60;
        long jitter = RND.nextInt(60);
        return LocalDateTime.now().plusSeconds(base + jitter);
    }

    // 단건 주문 생성 트랜잭션
    @Transactional
    public Long saveOrderInTransaction(Long memberId, ProductInternal product, Long quantity) {
        Order order = orderFactory.createOrder(memberId, product, quantity);
        order.setExpiresAt(holdUntil());
        orderRepository.save(order);
        return order.getId();
    }

    // 장바구니 주문 생성 트랜잭션
    @Transactional
    public Long saveOrderFromCartInTransaction(Long memberId, List<ProductInternal> products, Map<Long, Long> quantityMap) {
        Order order = orderFactory.createOrderFromCart(memberId, products, quantityMap);
        order.setExpiresAt(holdUntil());
        orderRepository.save(order);
        return order.getId();
    }

    // 결제 시도 상태 전이 트랜잭션
    @Transactional
    public void updateOrderStatusToProcessing(Order order) {
        order.updateOrderStatus(OrderStatus.PAYMENT_PROCESSING);
        order.setExpiresAt(holdUntil());
        orderRepository.save(order);
    }

    // 결제 완료 상태 전이 및 스케줄 등록
    @Transactional
    public void completeOrder(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_COMPLETE);
        order.setExpiresAt(null);
        orderRepository.save(order);

        final Long orderId = order.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    orderQuartzScheduler.scheduleAll(orderId);
                } catch (Exception e) {
                    throw new OrderScheduleFailedException("주문 스케줄 등록 실패", e);
                }
            }
        });
    }

    // 배송지 변경 트랜잭션
    @Transactional
    public void updateOrderAddressInTransaction(Order order, UpdateOrderAddressRequest request) {
        OrderAddress newAddress = new OrderAddress(
                request.getZipCode(),
                request.getStreetAddress(),
                request.getDetailAddress()
        );
        order.updateDeliveryAddress(newAddress);
        orderRepository.save(order);
    }

    // 주문 취소 상태 전이 트랜잭션
    @Transactional
    public void cancelOrderInTransaction(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_CANCEL);
        order.setExpiresAt(null);
        orderRepository.save(order);
    }

    // 반품 진행 상태 전이 트랜잭션
    @Transactional
    public void updateOrderStatusToReturning(Order order) {
        order.updateOrderStatus(OrderStatus.RETURNING);
        orderRepository.save(order);
    }

    // 반품 완료 상태 전이 트랜잭션
    @Transactional
    public void updateOrderStatusToReturnComplete(Order order) {
        order.updateOrderStatus(OrderStatus.RETURN_COMPLETE);
        orderRepository.save(order);
    }

    // 배송중 상태 전이 트랜잭션
    @Transactional
    public void updateOrderStatusToShipping(Order order) {
        order.updateOrderStatus(OrderStatus.DELIVERYING);
        orderRepository.save(order);
    }

    // 배송완료 상태 전이 트랜잭션
    @Transactional
    public void updateOrderStatusToDelivered(Order order) {
        order.updateOrderStatus(OrderStatus.DELIVERY_COMPLETE);
        orderRepository.save(order);
    }


    // 반품 불가 상태 전이 트랜잭션(인스턴스 보유 시)
    @Transactional
    public void updateOrderStatusToNonReturnable(Order order) {
        order.updateOrderStatus(OrderStatus.RETURN_NOT_PERMITTED);
        orderRepository.save(order);
    }
}
