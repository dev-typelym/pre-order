package com.app.preorder.common.exception.custom;

public class ProductClosedException extends RuntimeException {

    public ProductClosedException(String message) { super(message); }
    public ProductClosedException(String message, Throwable cause) { super(message, cause); }

}
