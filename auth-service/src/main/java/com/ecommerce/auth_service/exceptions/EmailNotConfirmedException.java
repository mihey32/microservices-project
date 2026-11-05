package com.ecommerce.auth_service.exceptions;

public class EmailNotConfirmedException extends RuntimeException {
    public EmailNotConfirmedException(String message) {
        super(message);
    }

    public EmailNotConfirmedException(String message, Throwable cause) {
        super(message, cause);
    }
}
