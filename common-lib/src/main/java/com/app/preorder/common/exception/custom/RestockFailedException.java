package com.app.preorder.common.exception.custom;

public class RestockFailedException extends RuntimeException {
    public RestockFailedException(String message) { super(message); }
}