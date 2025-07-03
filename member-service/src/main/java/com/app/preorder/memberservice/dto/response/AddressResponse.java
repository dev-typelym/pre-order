package com.app.preorder.memberservice.dto.response;

import com.app.preorder.infralib.util.EncryptUtil;
import com.app.preorder.memberservice.domain.vo.Address;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AddressResponse {
    private String roadAddress;
    private String detailAddress;
    private String postalCode;

    public static AddressResponse of(Address address, EncryptUtil encryptUtil) {
        return new AddressResponse(
                encryptUtil.decrypt(address.getRoadAddress()),
                encryptUtil.decrypt(address.getDetailAddress()),
                encryptUtil.decrypt(address.getPostalCode())
        );
    }
}
