package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AccountDto register(RegisterRequest request) {
       return accountService.register(request.email(), request.password());
    }

    public AuthResponse login(String email, String password) {
        /*String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);*/
        return null;
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }
}
