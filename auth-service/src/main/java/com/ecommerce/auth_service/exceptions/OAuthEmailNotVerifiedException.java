package com.ecommerce.auth_service.exceptions;

public class OAuthEmailNotVerifiedException extends RuntimeException {
    public OAuthEmailNotVerifiedException(String message) {
        super(message);
    }
}
