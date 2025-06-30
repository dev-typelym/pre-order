package com.app.preorder.orderservice.scheduler;

import com.app.preorder.orderservice.scheduler.job.OrderDeliveredJob;
import com.app.preorder.orderservice.scheduler.job.OrderNonReturnableJob;
import com.app.preorder.orderservice.scheduler.job.OrderShippingJob;
import com.app.preorder.orderservice.scheduler.job.ReturnProcessingJob;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.stereotype.Component;

@Slf4j
@DisallowConcurrentExecution
@Component
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

    public void scheduleOrderShipping(Long orderId) {
        scheduleJob(orderId, OrderShippingJob.class, 1, "OrderShipping");
    }

    public void scheduleOrderDelivered(Long orderId) {
        scheduleJob(orderId, OrderDeliveredJob.class, 2, "OrderDelivered");
    }

    public void scheduleReturnProcess(Long orderId) {
        scheduleJob(orderId, ReturnProcessingJob.class, 1, "ReturnProcessing");
    }

    public void scheduleNonReturnable(Long orderId) {
        scheduleJob(orderId, OrderNonReturnableJob.class, 3, "OrderNonReturnable");
    }

    public void cancelReturnProcess(Long orderId) {
        try {
            JobKey jobKey = new JobKey("ReturnProcessingJob-" + orderId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("[Scheduler] ReturnProcessingJob 취소 완료 - orderId: {}", orderId);
            } else {
                log.warn("[Scheduler] 취소 시도했으나 Job 존재하지 않음 - orderId: {}", orderId);
            }
        } catch (SchedulerException e) {
            log.error("[Scheduler] ReturnProcessingJob 취소 실패 - orderId: {}", orderId, e);
            throw new RuntimeException("스케줄 취소 실패", e);
        }
    }

    private void scheduleJob(Long orderId, Class<? extends Job> jobClass, int delayInDays, String keyPrefix) {
        if (orderId == null || orderId <= 0) {
            log.warn("[Scheduler] 유효하지 않은 orderId: {}", orderId);
            return;
        }

        try {
            JobKey jobKey = new JobKey(keyPrefix + "Job-" + orderId);
            if (scheduler.checkExists(jobKey)) {
                log.warn("[Scheduler] {} Job 이미 등록됨 - orderId: {}", keyPrefix, orderId);
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
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