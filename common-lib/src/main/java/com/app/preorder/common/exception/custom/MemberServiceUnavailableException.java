package com.app.preorder.common.exception.custom;

public class MemberServiceUnavailableException extends RuntimeException {
    public MemberServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    public MemberServiceUnavailableException(String message) { super(message); }
}