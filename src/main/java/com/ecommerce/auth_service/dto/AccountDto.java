package com.ecommerce.auth_service.dto;

import com.ecommerce.auth_service.model.Role;

public record AccountDto(
        Long id,
        String email,
        Role role,
        boolean blocked,
        boolean emailVerified
) {
}
