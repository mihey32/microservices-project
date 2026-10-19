package com.ecommerce.auth_service.exceptions;

public class InvalidRefreshTokenException extends InvalidCredentialsException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
