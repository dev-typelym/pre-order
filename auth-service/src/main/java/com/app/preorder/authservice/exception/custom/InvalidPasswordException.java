package com.app.preorder.authservice.exception.custom;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) { super(message); }
}