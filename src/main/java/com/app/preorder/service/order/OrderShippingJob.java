package com.app.preorder.service.order;

import com.app.preorder.entity.order.Order;
import com.app.preorder.repository.order.OrderRepository;
import com.app.preorder.type.OrderStatus;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderShippingJob implements Job{

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null && order.getStatus() == OrderStatus.ORDER_COMPLETE) {
            order.updateOrderStatus(OrderStatus.DELIVERYING);
            orderRepository.save(order);
        }
    }
}
