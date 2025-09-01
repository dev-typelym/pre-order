package com.app.preorder.common.type;

public enum OutboxStatus {
    NEW,    // DB에 적재됨 (아직 미발행)
    SENT,   // 브로커 전송 성공(ack 수신)
    FAILED  // 발행 실패(재시도 대상)
}
