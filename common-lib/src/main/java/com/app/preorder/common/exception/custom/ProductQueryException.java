package com.app.preorder.common.exception.custom;

public class ProductQueryException extends RuntimeException {
    public ProductQueryException(String message) { super(message); }
    public ProductQueryException(String message, Throwable cause) { super(message, cause); }
}
