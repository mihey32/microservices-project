package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.mapper.AccountMapper;
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
        var account = accountService.register(email, password);
        String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);

        return new AuthResponse(
                AccountMapper.mapToDto(account),
                accessToken,
                refreshToken
        );
    }

    public AuthResponse login(String email, String password) {
        return null;
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }
}
