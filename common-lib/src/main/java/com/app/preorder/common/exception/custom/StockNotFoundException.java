package com.app.preorder.common.exception.custom;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(String message) { super(message); }
}
