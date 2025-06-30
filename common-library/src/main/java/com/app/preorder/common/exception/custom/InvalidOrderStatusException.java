package com.app.preorder.common.exception.custom;

public class InvalidOrderStatusException extends RuntimeException {
    public InvalidOrderStatusException(String message) { super(message); }
}
