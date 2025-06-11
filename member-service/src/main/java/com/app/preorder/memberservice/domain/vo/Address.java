package com.app.preorder.memberservice.domain.vo;

import com.app.preorder.infralib.util.EncryptUtil;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address implements Serializable {

    private String address;
    private String addressDetail;
    private String addressSubDetail;
    private String postcode;

    public Address(String address, String addressDetail, String addressSubDetail, String postcode) {
        this.address = address;
        this.addressDetail = addressDetail;
        this.addressSubDetail = addressSubDetail;
        this.postcode = postcode;
    }

    public Address encryptWith(EncryptUtil encryptUtil) {
        return new Address(
                encryptUtil.encrypt(this.address),
                encryptUtil.encrypt(this.addressDetail),
                encryptUtil.encrypt(this.addressSubDetail),
                encryptUtil.encrypt(this.postcode)
        );
    }
}
