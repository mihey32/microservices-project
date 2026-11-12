package com.ecommerce.auth_service.dto.request;

import com.ecommerce.auth_service.validation.AllowedPasswordChars;
import com.ecommerce.auth_service.validation.PasswordSize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record RegisterRequest(
        @Email
        @NotBlank
        String email,
        @NotBlank
        @PasswordSize(min = 8, max = 72)
        @AllowedPasswordChars
        String password
) {
}
