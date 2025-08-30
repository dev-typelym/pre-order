package com.app.preorder.common.exception.custom;

public class MemberQueryException extends RuntimeException {
    public MemberQueryException(String message) { super(message); }
    public MemberQueryException(String message, Throwable cause) { super(message, cause); }
}