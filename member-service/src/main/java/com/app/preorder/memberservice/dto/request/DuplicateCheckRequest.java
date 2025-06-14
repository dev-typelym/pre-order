package com.app.preorder.memberservice.dto.request;

import lombok.Getter;

@Getter
public class DuplicateCheckRequest {
    private String type;   // "LOGIN_ID", "EMAIL", "PHONE"
    private String value;
}