package com.app.preorder.orderservice.scheduler.job;

import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderShippingJob implements Job {

    private final OrderService orderService;

    @Override
    public void execute(JobExecutionContext context) throws org.quartz.JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        try {
            orderService.updateOrderStatusShipping(orderId);
        } catch (Exception e) {
            System.err.println("OrderShippingJob 처리 중 오류 발생: " + e.getMessage());
            throw new org.quartz.JobExecutionException("OrderShippingJob 실행 실패", e);
        }
    }
}
