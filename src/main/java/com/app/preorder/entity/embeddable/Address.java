package com.app.preorder.entity.embeddable;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@ToString
public class Address implements Serializable {

    private String address;
    private String addressDetail;
    private String addressSubDetail;
    private String postcode;

    // 기본 생성자 생성(Setter를 쓰면 값이 변경 가능해지므로 생성자로 값을 지정)
    protected Address(){

    } // public보다 protected가 더 안전

    public Address(String address, String addressDetail, String addressSubDetail, String postcode) {
        this.address = address;
        this.addressDetail = addressDetail;
        this.addressSubDetail = addressSubDetail;
        this.postcode = postcode;
    }
}
