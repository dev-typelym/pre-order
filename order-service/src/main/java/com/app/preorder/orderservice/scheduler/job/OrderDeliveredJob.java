package com.app.preorder.orderservice.scheduler.job;

import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class OrderDeliveredJob implements Job {

    private final OrderService orderService;

    @Override
    public void execute(JobExecutionContext context) throws org.quartz.JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        try {
            orderService.updateOrderStatusDelivered(orderId);
        } catch (Exception e) {
            // 로그 출력
            System.err.println("OrderDeliveredJob 처리 중 오류 발생: " + e.getMessage());

            // Quartz에 실패 신호 전달
            throw new org.quartz.JobExecutionException("OrderDeliveredJob 실행 실패", e);
        }
    }
}
