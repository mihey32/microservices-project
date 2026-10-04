package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.exceptions.InvalidCredentialsException;
import com.ecommerce.auth_service.exceptions.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AccountDto register(RegisterRequest request) {
       return accountService.register(request.email(), request.password());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        var account = accountService.login(loginRequest.email(), loginRequest.password());
        String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);
        log.info("Login success: accountId={}", account.id());
        return new AuthResponse(account, refreshToken, accessToken);
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }

    public AuthResponse refresh(String refreshToken) {
        Long accountId = refreshTokenService.consume(refreshToken)
                .orElseThrow(() -> new InvalidRefreshTokenException("invalid refresh token"));



        return null; //заглушка
    }
}
