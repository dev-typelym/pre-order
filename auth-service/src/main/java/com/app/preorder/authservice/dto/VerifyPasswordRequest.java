package com.app.preorder.authservice.dto;

import lombok.Data;

@Data
public class VerifyPasswordRequest {
    private String username;
    private String password;
}