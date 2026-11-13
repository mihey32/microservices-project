package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.EmailNotificationEvent;
import com.ecommerce.auth_service.dto.UserCreatedEvent;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.exceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AccountService accountService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService resetService;
    private final EmailVerificationService emailVerificationService;
    private final OutboxService outboxService;

    @SuppressWarnings("LoggingSimilarMessage")
    @Transactional
    public AccountDto register(RegisterRequest request) {
        AccountDto accountDto = accountService.register(request.email(), request.password());
        String verificationToken = emailVerificationService.generateVerificationToken(accountDto);

        UserCreatedEvent userCreatedEvent = new UserCreatedEvent(accountDto.id(), accountDto.email(), Instant.now());

        EmailNotificationEvent notificationEvent = new EmailNotificationEvent(
                accountDto.id(), accountDto.email(), verificationToken, Instant.now());

        outboxService.saveEvent("Account", accountDto.id().toString(), "user.created", userCreatedEvent);
        outboxService.saveEvent("Account", accountDto.id().toString(), "email.verification-requested", notificationEvent);


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

    @Transactional
    public void resendVerificationEmail(String email) {
        AccountDto account = accountService.findAccountByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("account not found"));

        if (account.emailVerified()) {
            throw new ConflictException("email already verified");
        }

        emailVerificationService.invalidatePreviousTokens(account.id());
        String verificationToken = emailVerificationService.generateVerificationToken(account);

        EmailNotificationEvent event = new EmailNotificationEvent(
                account.id(), account.email(), verificationToken, Instant.now());
        outboxService.saveEvent("Account", account.id().toString(), "email.verification-requested", event);

        log.info("Resent verification email for accountId={}", account.id());
    }

    public AuthResponse oauthLogin(AccountDto account) {
        assertNotBlocked(account);
        String accessToken = jwtService.generateToken(account);
        String refreshToken = refreshTokenService.generateRefreshToken(account);

        log.info("OAuth login success: accountId={}", account.id());
        return new AuthResponse(account, refreshToken, accessToken);
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
                    EmailNotificationEvent event = new EmailNotificationEvent(
                            accountDto.id(),
                            accountDto.email(),
                            rawToken,
                            Instant.now()
                    );
                    outboxService.saveEvent("Account", accountDto.id().toString(), "password.reset-requested", event);

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
