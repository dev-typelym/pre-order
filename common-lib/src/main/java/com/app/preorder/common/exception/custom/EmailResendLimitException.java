package com.app.preorder.common.exception.custom;

public class EmailResendLimitException extends RuntimeException {
    public EmailResendLimitException(String message) { super(message); }
    }
