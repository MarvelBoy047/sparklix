package com.sparklix.adminservice.exception;

public class OperationConflictException extends RuntimeException {
    public OperationConflictException(String message) {
        super(message);
    }
}