package com.app.preorder.memberservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class VerifyEmailRequest {
    private String loginId;
}
