package com.app.preorder.memberservice.exception;

public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) { super(message); }
}
