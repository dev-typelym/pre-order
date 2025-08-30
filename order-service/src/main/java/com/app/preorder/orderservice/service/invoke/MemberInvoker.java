package com.app.preorder.orderservice.service.invoke;

import com.app.preorder.common.dto.MemberInternal;
import com.app.preorder.common.exception.custom.MemberQueryException;
import com.app.preorder.orderservice.client.MemberServiceClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberInvoker {

    private final MemberServiceClient memberServiceClient;

    @CircuitBreaker(name = "memberQuery", fallbackMethod = "memberFallback")
    @Retry(name = "memberQuery")
    public MemberInternal getMember(Long memberId) {
        return memberServiceClient.getMemberById(memberId);
    }

    @CircuitBreaker(name = "memberQuery", fallbackMethod = "memberFallback")
    @Retry(name = "memberQuery")
    public MemberInternal getByUsername(String username) {
        return memberServiceClient.getMemberByUsername(username);
    }

    // fallback 오버로드
    private MemberInternal memberFallback(Long memberId, Throwable ex) {
        throw new MemberQueryException("회원 조회 실패 (memberId=" + memberId + ")", ex);
    }

    private MemberInternal memberFallback(String username, Throwable ex) {
        throw new MemberQueryException("회원 조회 실패 (username=" + username + ")", ex);
    }
}
