package com.app.preorder.orderservice.service.scheduler;

import com.app.preorder.orderservice.service.OrderDeliveredJob;
import com.app.preorder.orderservice.service.OrderNonReturnableJob;
import com.app.preorder.orderservice.service.OrderShippingJob;
import com.app.preorder.orderservice.service.ReturnProcessingJob;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderScheduler {

    private Scheduler scheduler;

    @PostConstruct
    public void init() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            log.error("Scheduler 초기화 실패", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            log.error("Scheduler 종료 실패", e);
        }
    }

    public void scheduleAll(Long orderId) {
        scheduleOrderShipping(orderId);
        scheduleOrderDelivered(orderId);
        scheduleNonReturnable(orderId);
    }

    // 주문상태 배달중으로 바꾸는 스케쥴링 메소드
    public void scheduleOrderShipping(Long orderId) {
        scheduleJob(orderId, OrderShippingJob.class, 1, "OrderShipping");
    }

    // 주문상태 배달중에서 배달완료로 바꾸는 스케쥴링 메소드
    public void scheduleOrderDelivered(Long orderId) {
        scheduleJob(orderId, OrderDeliveredJob.class, 2, "OrderDelivered");
    }

    // 반품 신청후 처리를 스케줄링 하는 메소드
    public void scheduleReturnProcess(Long orderId) {
        scheduleJob(orderId, ReturnProcessingJob.class, 1, "ReturnProcessing");
    }

    // 배송완료후 1일 이후엔 배송상태를 반품 불가로 바꾸는 메소드
    public void scheduleNonReturnable(Long orderId) {
        scheduleJob(orderId, OrderNonReturnableJob.class, 3, "OrderNonReturnable");
    }

    private void scheduleJob(Long orderId, Class<? extends Job> jobClass, int delayInDays, String keyPrefix) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(keyPrefix + "Job-" + orderId)
                    .usingJobData("orderId", orderId)
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(keyPrefix + "Trigger-" + orderId)
                    .startAt(DateBuilder.futureDate(delayInDays, DateBuilder.IntervalUnit.DAY))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            log.error("[Scheduler] {} 등록 실패 - orderId: {}", keyPrefix, orderId, e);
        }
    }
}