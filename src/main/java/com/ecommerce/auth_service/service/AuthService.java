package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.exceptions.*;
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
    private final PasswordResetService resetService;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public AccountDto register(RegisterRequest request) {
        AccountDto accountDto = accountService.register(request.email(), request.password());
        String verificationToken = emailVerificationService.generateVerificationToken(accountDto);
        // TODO: publish user.created event to Kafka
        log.debug("DEV ONLY - verification token for {} = {}", accountDto.id(), verificationToken);
        return accountDto;
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

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        Long accountId = emailVerificationService.consumeVerificationToken(request.token())
                .orElseThrow(() -> {
                    log.debug("Email verification token invalid, expired or already used");
                    return new InvalidVerificationTokenException("invalid or expired verification token");
                });
        accountService.markEmailVerified(accountId);
        log.info("Email verified: accountId={}", accountId);
    }

    public AuthResponse oauthLogin(String oauth2Provider, String providerId, String email) {
        return null;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, String email) {
        AccountDto accountDto = accountService.findAccountByEmail(email)
                .orElseThrow(() -> {
                    log.debug("Account not found");
                    return new InvalidCredentialsException("account not found");
                });
        Long accountId = accountDto.id();

        if (!accountService.verifyPassword(accountId, request.oldPassword())) {
            log.debug("Invalid old password");
            throw new InvalidCredentialsException("invalid credentials");
        }

        if (request.oldPassword().equals(request.newPassword())) {
            throw new InvalidCredentialsException("new password must differ from old");
        }

        accountService.changePassword(accountId, request.newPassword());
        refreshTokenService.revokeAllByAccountId(accountId);
        log.info("Password changed: accountId={}", accountId);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest refreshRequest) {
        Long accountId = refreshTokenService.consume(refreshRequest.refreshToken())
                .orElseThrow(() -> {
                    log.debug("Refresh token invalid or already consumed");
                    return new InvalidRefreshTokenException("invalid refresh token");
                });

        AccountDto account = accountService.findAccountById(accountId)
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

    public void logout(RefreshRequest refreshRequest) {
        refreshTokenService.revokeByToken(refreshRequest.refreshToken());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        accountService.findAccountByEmail(request.email())
                .ifPresent(accountDto -> {
                    resetService.invalidatePreviousTokens(accountDto.id());
                    String rawToken = resetService.generateResetToken(accountDto);
                    log.debug("DEV ONLY - reset token for {} = {}", accountDto.id(), rawToken);
                    // TODO: publish event to Kafka
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Long accountId = resetService.consumeResetToken(request.token())
                .orElseThrow(() -> new InvalidResetTokenException("invalid or expired reset token"));
        accountService.changePassword(accountId, request.newPassword());
        refreshTokenService.revokeAllByAccountId(accountId);
        log.info("Reset password: accountId={}", accountId);
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
