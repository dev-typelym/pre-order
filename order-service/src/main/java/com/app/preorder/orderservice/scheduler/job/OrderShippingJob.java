package com.app.preorder.orderservice.scheduler.job;

import com.app.preorder.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderShippingJob implements Job {

    private final OrderService orderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Long orderId = context.getJobDetail().getJobDataMap().getLong("orderId");
        if (orderId == null || orderId <= 0) {
            log.warn("유효하지 않은 orderId: {}", orderId);
            return;
        }
        try {
            orderService.updateOrderStatusShipping(orderId);
        } catch (Exception e) {
            log.error("OrderShippingJob 처리 중 오류 발생 - orderId: {}, 이유: {}", orderId, e.getMessage(), e);
            throw new JobExecutionException("OrderShippingJob 실행 실패", e);
        }
    }
}
