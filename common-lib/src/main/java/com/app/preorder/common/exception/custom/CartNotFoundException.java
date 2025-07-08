package com.app.preorder.common.exception.custom;

public class CartNotFoundException extends RuntimeException {
    public CartNotFoundException(String message) { super(message); }
}