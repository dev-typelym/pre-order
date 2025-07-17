package com.app.preorder.common.exception.custom;

public class OrderScheduleFailedException extends RuntimeException {
    public OrderScheduleFailedException(String message) { super(message); }
    public OrderScheduleFailedException(String message, Throwable cause) { super(message, cause); }
}
