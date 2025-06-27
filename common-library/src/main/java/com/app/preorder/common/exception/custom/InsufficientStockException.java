package com.app.preorder.common.exception.custom;

public class InsufficientStockException extends RuntimeException{
    public InsufficientStockException(String message) { super(message); }
}
