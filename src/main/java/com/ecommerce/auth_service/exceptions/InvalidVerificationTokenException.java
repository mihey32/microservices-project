package com.ecommerce.auth_service.exceptions;

public class InvalidVerificationTokenException extends InvalidCredentialsException {
    public InvalidVerificationTokenException(String message) {
        super(message);
    }
}
