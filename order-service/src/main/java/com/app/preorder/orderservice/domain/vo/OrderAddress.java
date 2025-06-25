package com.app.preorder.orderservice.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderAddress {

    private String roadAddress;      // 도로명 주소
    private String detailAddress;    // 상세 주소
    private String postalCode;       // 우편번호

    public OrderAddress(String roadAddress, String detailAddress, String postalCode) {
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
    }
}
