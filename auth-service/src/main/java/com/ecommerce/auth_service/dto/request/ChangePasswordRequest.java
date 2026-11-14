package com.ecommerce.auth_service.dto.request;

import com.ecommerce.auth_service.validation.AllowedPasswordChars;
import com.ecommerce.auth_service.validation.PasswordSize;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank
        String oldPassword,
        @NotBlank
        @PasswordSize(min = 8, max = 72)
        @AllowedPasswordChars
        String newPassword
) {
}
