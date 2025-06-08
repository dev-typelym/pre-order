package com.app.preorder.authservice.exception.custom;

public class ForbiddenException extends RuntimeException{
    public ForbiddenException(String message) { super(message); }
}
