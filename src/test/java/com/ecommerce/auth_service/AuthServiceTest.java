package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.exceptions.*;
import com.ecommerce.auth_service.repository.AccountRepository;
import com.ecommerce.auth_service.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Transactional
public class AuthServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private AccountRepository accountRepository;

    private String uniqueEmail() {
        return "authTest-" + UUID.randomUUID() + "@mail.com";
    }

    private AuthResponse login(AccountDto account, String password) {
        return authService.login(new LoginRequest(account.email(), password));
    }

    private AccountDto registerVerified(String email) {
        AccountDto account = accountService.register(email, "qwerty1234");
        accountService.markEmailVerified(account.id());
        return accountService.findAccountById(account.id()).orElseThrow();
    }

    private void blockAccount(Long accountId) {
        var account = accountRepository.findById(accountId).orElseThrow();
        account.setBlocked(true);
        accountRepository.saveAndFlush(account);
    }

    @Test
    void login_validCredentials_returnsTokensWithClaims() {
        AccountDto account = registerVerified(uniqueEmail());

        AuthResponse response = login(account, "qwerty1234");

        assertThat(response.account().id()).isEqualTo(account.id());
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(jwtService.isTokenValid(response.accessToken())).isTrue();
        assertThat(jwtService.extractId(response.accessToken())).isEqualTo(account.id());
        assertThat(jwtService.extractEmail(response.accessToken())).isEqualTo(account.email());
        assertThat(jwtService.extractRole(response.accessToken())).isEqualTo("ROLE_USER");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        AccountDto account = registerVerified(uniqueEmail());

        assertThatThrownBy(() -> login(account, "wrongPass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unverifiedEmail_throwsEmailNotConfirmed() {
        AccountDto account = accountService.register(uniqueEmail(), "qwerty1234");

        assertThatThrownBy(() -> login(account, "qwerty1234"))
                .isInstanceOf(EmailNotConfirmedException.class);
    }

    @Test
    void login_blockedAccount_throwsAccountBlocked() {
        AccountDto account = registerVerified(uniqueEmail());
        blockAccount(account.id());

        assertThatThrownBy(() -> login(account, "qwerty1234"))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void oauthLogin_validAccount_returnsTokensWithoutEmailVerification() {
        AccountDto account = accountService.register("o" + uniqueEmail(), "qwerty1234");

        AuthResponse response = authService.oauthLogin(account);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(jwtService.extractId(response.accessToken())).isEqualTo(account.id());
    }

    @Test
    void oauthLogin_blockedAccount_throwsAccountBlocked() {
        AccountDto account = accountService.register("o" + uniqueEmail(), "qwerty1234");
        blockAccount(account.id());
        AccountDto refreshed = accountService.findAccountById(account.id()).orElseThrow();

        assertThatThrownBy(() -> authService.oauthLogin(refreshed))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void refresh_validToken_rotatesAndConsumesOld() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse initial = login(account, "qwerty1234");

        AuthResponse rotated = authService.refresh(new RefreshRequest(initial.refreshToken()));

        assertThat(rotated.accessToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotBlank();
        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(initial.refreshToken())))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_unknownToken_throwsInvalidRefreshToken() {
        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(UUID.randomUUID().toString())))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refresh_blockedAccount_throwsAccountBlocked() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse tokens = login(account, "qwerty1234");
        blockAccount(account.id());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest(tokens.refreshToken())))
                .isInstanceOf(AccountBlockedException.class);
    }

    @Test
    void changePassword_valid_rotatesPasswordAndRevokesAllRefresh() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse tokens = login(account, "qwerty1234");

        authService.changePassword(
                new ChangePasswordRequest("qwerty1234", "newPass12345"),
                account.email());

        assertThatThrownBy(() -> accountService.authenticate(account.email(), "qwerty1234"))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(accountService.authenticate(account.email(), "newPass12345").id())
                .isEqualTo(account.id());

        assertThat(refreshTokenService.consume(tokens.refreshToken())).isEmpty();
    }

    @Test
    void changePassword_wrongOldPassword_throws() {
        AccountDto account = registerVerified(uniqueEmail());

        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("wrongOld", "newPass12345"), account.email()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("invalid credentials");
    }

    @Test
    void changePassword_newSameAsOld_throws() {
        AccountDto account = registerVerified(uniqueEmail());

        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("qwerty1234", "qwerty1234"), account.email()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("new password must differ from old");
    }

    @Test
    void changePassword_unknownEmail_throws() {
        assertThatThrownBy(() -> authService.changePassword(
                new ChangePasswordRequest("qwerty1234", "newPass12345"),
                "unknown-" + UUID.randomUUID() + "@mail.com"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("account not found");
    }

    @Test
    void logout_revokesRefreshToken() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse tokens = login(account, "qwerty1234");

        authService.logout(new RefreshRequest(tokens.refreshToken()));

        assertThat(refreshTokenService.consume(tokens.refreshToken())).isEmpty();
    }

    @Test
    void logout_alreadyRevoked_isIdempotent() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse tokens = login(account, "qwerty1234");

        authService.logout(new RefreshRequest(tokens.refreshToken()));

        assertThatCode(() -> authService.logout(new RefreshRequest(tokens.refreshToken())))
                .doesNotThrowAnyException();
    }

    @Test
    void verifyEmail_validToken_marksAccountVerified() {
        AccountDto account = accountService.register(uniqueEmail(), "qwerty1234");
        assertThat(account.emailVerified()).isFalse();
        String rawToken = emailVerificationService.generateVerificationToken(account);

        authService.verifyEmail(new VerifyEmailRequest(rawToken));

        AccountDto verified = accountService.findAccountById(account.id()).orElseThrow();
        assertThat(verified.emailVerified()).isTrue();
    }

    @Test
    void verifyEmail_invalidToken_throwsInvalidVerificationToken() {
        assertThatThrownBy(() -> authService.verifyEmail(
                new VerifyEmailRequest(UUID.randomUUID().toString())))
                .isInstanceOf(InvalidVerificationTokenException.class);
    }

    @Test
    void resetPassword_validToken_changesPasswordAndRevokesRefresh() {
        AccountDto account = registerVerified(uniqueEmail());
        AuthResponse tokens = login(account, "qwerty1234");
        String rawToken = passwordResetService.generateResetToken(account);

        authService.resetPassword(new ResetPasswordRequest(rawToken, "newPass12345"));

        assertThatThrownBy(() -> accountService.authenticate(account.email(), "qwerty1234"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(accountService.authenticate(account.email(), "newPass12345").id())
                .isEqualTo(account.id());
        assertThat(refreshTokenService.consume(tokens.refreshToken())).isEmpty();
    }

    @Test
    void resetPassword_invalidToken_throwsInvalidResetToken() {
        assertThatThrownBy(() -> authService.resetPassword(
                new ResetPasswordRequest(UUID.randomUUID().toString(), "newPass12345")))
                .isInstanceOf(InvalidResetTokenException.class);
    }
}
