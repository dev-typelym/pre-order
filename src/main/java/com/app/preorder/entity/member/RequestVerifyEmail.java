package com.app.preorder.entity.member;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
public class RequestVerifyEmail {
    String username;

    public RequestVerifyEmail(String username) {
        this.username = username;
    }
}
