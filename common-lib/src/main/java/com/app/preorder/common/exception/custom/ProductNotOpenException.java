package com.app.preorder.common.exception.custom;

public class ProductNotOpenException extends RuntimeException {

    public ProductNotOpenException(String message) { super(message); }
    public ProductNotOpenException(String message, Throwable cause) { super(message, cause); }

}
