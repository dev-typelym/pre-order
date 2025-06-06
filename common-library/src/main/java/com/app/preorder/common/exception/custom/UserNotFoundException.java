package com.app.preorder.common.exception.custom;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) { super(message); }
}