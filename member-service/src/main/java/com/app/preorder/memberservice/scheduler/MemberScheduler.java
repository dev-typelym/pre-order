package com.app.preorder.memberservice.scheduler;

import com.app.preorder.infralib.util.RedisUtil;
import com.app.preorder.memberservice.client.CartServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class MemberScheduler {

    private final RedisUtil redisUtil;
    private final CartServiceClient cartServiceClient;

    private static final long BASE_DELAY_SECONDS = 2;        // 초기 딜레이 2초
    private static final long MAX_DELAY_SECONDS = 300;       // 최대 딜레이 300초 (5분)
    private static final int MAX_ATTEMPTS = 5;               // 최대 시도 횟수

    @Scheduled(fixedDelay = 3000)
    public void retryFailedCarts() {
        Set<String> failedIds = redisUtil.getSetMembers("failed_cart_member_ids");
        if (failedIds == null || failedIds.isEmpty()) {
            return;
        }

        for (String idStr : failedIds) {
            Long memberId = Long.parseLong(idStr);
            String attemptKey = "failed_cart_member_ids:" + idStr + ":attempt";
            String nextKey = "failed_cart_member_ids:" + idStr + ":next";

            int attempt = 0;
            String attemptStr = redisUtil.getData(attemptKey);
            if (attemptStr != null) {
                attempt = Integer.parseInt(attemptStr);
            }

            String nextRetryStr = redisUtil.getData(nextKey);
            long currentTimestamp = System.currentTimeMillis() / 1000;

            if (nextRetryStr != null) {
                long nextRetry = Long.parseLong(nextRetryStr);
                if (currentTimestamp < nextRetry) {
                    // 아직 재시도할 시간 아님
                    continue;
                }
            }

            try {
                cartServiceClient.createCart(memberId);
                // 성공 시: Redis 데이터 정리
                redisUtil.removeSetMember("failed_cart_member_ids", idStr);
                redisUtil.deleteData(attemptKey);
                redisUtil.deleteData(nextKey);
                log.info("카트 재시도 성공: {}", memberId);
            } catch (Exception e) {
                log.error("카트 재시도 실패: {}", memberId, e);

                attempt++;
                if (attempt >= MAX_ATTEMPTS) {
                    // 최대 시도 초과 시: 실패 로그만 남기고 제거
                    redisUtil.removeSetMember("failed_cart_member_ids", idStr);
                    redisUtil.deleteData(attemptKey);
                    redisUtil.deleteData(nextKey);
                    log.warn("최대 재시도 초과. memberId: {}", memberId);
                    continue;
                }

                // 지연 시간 계산
                long delay = Math.min(BASE_DELAY_SECONDS * (1L << (attempt - 1)), MAX_DELAY_SECONDS);
                long nextRetryTime = currentTimestamp + delay;

                // 다음 시도 정보 저장
                redisUtil.setData(attemptKey, String.valueOf(attempt));
                redisUtil.setData(nextKey, String.valueOf(nextRetryTime));

                log.info("다음 재시도 예약. memberId: {}, attempt: {}, delay: {}초", memberId, attempt, delay);
            }
        }
    }
}
