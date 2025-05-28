package com.app.preorder.memberservice.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestLoginUserDTO {
    private String username;
    private String password;

    public RequestLoginUserDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

