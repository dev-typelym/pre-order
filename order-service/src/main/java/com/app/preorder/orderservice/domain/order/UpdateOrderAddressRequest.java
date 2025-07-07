package com.app.preorder.orderservice.domain.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderAddressRequest {
    private String zipCode;
    private String streetAddress;
    private String detailAddress;
}
