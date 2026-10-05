package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.request.LoginRequest;
import com.ecommerce.auth_service.dto.request.RefreshRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.exceptions.AccountBlockedException;
import com.ecommerce.auth_service.exceptions.EmailNotConfirmedException;
import com.ecommerce.auth_service.exceptions.InvalidRefreshTokenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var account = accountService.authenticate(loginRequest.email(), loginRequest.password());

        assertNotBlocked(account);
        assertEmailVerified(account);

        String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);

        log.info("Login success: accountId={}", account.id());
        return new AuthResponse(account, refreshToken, accessToken);
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest refreshRequest) {
        Long accountId = refreshTokenService.consume(refreshRequest.refreshToken())
                .orElseThrow(() -> {
                    log.debug("Refresh token invalid or already consumed");
                    return new InvalidRefreshTokenException("invalid refresh token");
                });

        AccountDto account = accountService.findById(accountId)
                .orElseThrow(() -> {
                    log.debug("Refresh token points to non-existent account: accountId={}", accountId);
                    return new InvalidRefreshTokenException("invalid refresh token");
                });
        log.debug("Refresh attempt: accountId={}", accountId);
        assertNotBlocked(account);

        String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);

        log.debug("Refresh success: accountId={}", account.id());

        return new AuthResponse(account, refreshToken, accessToken);
    }


    private void assertNotBlocked(AccountDto account) {
        if (account.blocked()) {
            log.warn("Blocked account access attempt: accountId={}", account.id());
            throw new AccountBlockedException("account is blocked");
        }
    }

    private void assertEmailVerified(AccountDto account) {
        if (!account.emailVerified()) {
            log.warn("Unverified email access attempt: accountId={}", account.id());
            throw new EmailNotConfirmedException("email is not verified");
        }
    }
}
