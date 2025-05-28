package com.app.preorder.orderservice.service;

import com.app.preorder.entity.order.Order;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.type.OrderStatus;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderNonReturnableJob  implements Job {

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.DELIVERY_COMPLETE) {
            order.updateOrderStatus(OrderStatus.RETURN_NOT_PERMITTED);
            orderRepository.save(order);
        }
    }
}
