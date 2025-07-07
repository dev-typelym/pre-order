package com.app.preorder.memberservice.domain.vo;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Address implements Serializable {

    private String roadAddress;
    private String detailAddress;
    private String postalCode;

    public Address(String roadAddress, String detailAddress, String postalCode) {
        this.roadAddress = roadAddress;
        this.detailAddress = detailAddress;
        this.postalCode = postalCode;
    }

}
