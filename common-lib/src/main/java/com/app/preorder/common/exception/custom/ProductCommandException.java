package com.app.preorder.common.exception.custom;

public class ProductCommandException extends RuntimeException {
    public ProductCommandException(String message) { super(message); }
    public ProductCommandException(String message, Throwable cause) { super(message, cause); }
}
