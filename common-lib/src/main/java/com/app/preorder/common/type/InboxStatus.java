package com.app.preorder.common.type;

public enum InboxStatus {
    PENDING,    // 수신 후 처리 대기
    PROCESSED,  // 비즈니스 처리 완료(멱등 OK)
    FAILED      // 처리 실패(재시도 대상)
}
