package com.ecommerce.auth_service.exceptions;

public class InvalidResetTokenException extends InvalidCredentialsException {
    public InvalidResetTokenException(String message) {
        super(message);
    }
}
