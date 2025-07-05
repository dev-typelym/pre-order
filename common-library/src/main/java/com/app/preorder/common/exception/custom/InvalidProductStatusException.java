package com.app.preorder.common.exception.custom;

public class InvalidProductStatusException extends RuntimeException {
    public InvalidProductStatusException(String message) { super(message); }
}
