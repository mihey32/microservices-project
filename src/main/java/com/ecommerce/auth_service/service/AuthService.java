package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.model.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(String email, String password) {
        return null;
    }

    public AuthResponse login(String email, String password) {
        return null;
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }
}
