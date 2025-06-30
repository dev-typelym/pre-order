package com.app.preorder.orderservice.scheduler.job;

import com.app.preorder.orderservice.service.OrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public class OrderDeliveredJob implements Job {

    private final OrderService orderService;

    public OrderDeliveredJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        orderService.updateOrderStatusDelivered(orderId);
    }
}
