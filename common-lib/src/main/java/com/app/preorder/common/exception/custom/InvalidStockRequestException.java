package com.app.preorder.common.exception.custom;

public class InvalidStockRequestException extends RuntimeException {
    public InvalidStockRequestException(String message) { super(message); }
}