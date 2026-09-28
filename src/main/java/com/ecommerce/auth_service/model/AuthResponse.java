package com.ecommerce.auth_service.model;

import com.ecommerce.auth_service.dto.AccountDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private AccountDto account;
    private String refreshToken;
    private String accessToken;
}
