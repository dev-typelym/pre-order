package com.app.preorder.common.exception.custom;

public class FeignException extends RuntimeException{
    public FeignException(String message) { super(message); }
}
