package com.app.preorder.common.exception.custom;

public class ProductAlreadyHasStockException extends RuntimeException {
    public ProductAlreadyHasStockException(String message) { super(message); }
}
