package com.app.preorder.orderservice.service;

import com.app.preorder.common.dto.ProductInternal;
import com.app.preorder.common.exception.custom.OrderScheduleFailedException;
import com.app.preorder.common.type.OrderStatus;
import com.app.preorder.orderservice.domain.order.UpdateOrderAddressRequest;
import com.app.preorder.orderservice.domain.vo.OrderAddress;
import com.app.preorder.orderservice.entity.Order;
import com.app.preorder.orderservice.entity.OrderItem;
import com.app.preorder.orderservice.factory.OrderFactory;
import com.app.preorder.orderservice.repository.OrderRepository;
import com.app.preorder.orderservice.scheduler.OrderScheduler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.SchedulingException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class OrderTransactionalService {

    private final OrderRepository orderRepository;
    private final OrderScheduler orderScheduler;
    private final OrderFactory orderFactory;


    @Transactional
    public Long saveOrderInTransaction(Long memberId, ProductInternal product, Long quantity) {
        Order order = orderFactory.createOrder(memberId, product, quantity);
        orderRepository.save(order);
        return order.getId();
    }

    @Transactional
    public Long saveOrderFromCartInTransaction(Long memberId, List<ProductInternal> products, Map<Long, Long> quantityMap) {
        Order order = orderFactory.createOrderFromCart(memberId, products, quantityMap);
        orderRepository.save(order);
        return order.getId();
    }

    @Transactional
    public void completeOrder(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_COMPLETE);
        orderRepository.save(order);
        try {
            orderScheduler.scheduleAll(order.getId());
        } catch (SchedulingException e) {
            throw new OrderScheduleFailedException("주문 스케줄 등록 실패", e);
        }
    }

    @Transactional
    public void updateOrderAddressInTransaction(Order order, UpdateOrderAddressRequest request) {
        OrderAddress newAddress = new OrderAddress(
                request.getZipCode(),
                request.getStreetAddress(),
                request.getDetailAddress()
        );

        order.updateDeliveryAddress(newAddress);
    }

    @Transactional
    public void cancelOrderInTransaction(Order order) {
        order.updateOrderStatus(OrderStatus.ORDER_CANCEL);
        orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatusToReturning(Order order) {
        order.updateOrderStatus(OrderStatus.RETURNING);
        orderRepository.save(order);
    }

    @Transactional
    public void updateOrderStatusToReturnComplete(Order order) {
        order.updateOrderStatus(OrderStatus.RETURN_COMPLETE);
        orderRepository.save(order);
    }
}
