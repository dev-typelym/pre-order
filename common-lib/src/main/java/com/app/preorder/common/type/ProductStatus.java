package com.app.preorder.common.type;

public enum ProductStatus {
    ENABLED,    // 판매 가능
    DISABLED,   // 관리자가 비활성화
    SOLD_OUT,   // 재고 없음
    COMING_SOON // 오픈 전
}
