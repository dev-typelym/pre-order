package com.app.preorder.memberservice.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RequestVerifyEmailDTO {
    String username;

    public RequestVerifyEmailDTO(String username) {
        this.username = username;
    }
}
