package com.app.preorder.orderservice.scheduler.job;


import com.app.preorder.orderservice.service.OrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

public class ReturnProcessingJob implements Job {

    private final OrderService orderService;

    public ReturnProcessingJob(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        orderService.processReturn(orderId);
    }
}
