package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.exceptions.InvalidCredentialsException;
import com.ecommerce.auth_service.exceptions.InvalidResetTokenException;
import com.ecommerce.auth_service.exceptions.InvalidVerificationTokenException;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.repository.AccountRepository;
import com.ecommerce.auth_service.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
public class AccountServiceTest extends AbstractIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_shouldCreateAccountWithNormalizedEmailAndHashedPassword() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        AccountDto result = accountService.register(email, "qwerty1234");

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo(normalizeEmail(email));
        assertThat(result.role()).isEqualTo(Role.ROLE_USER);
        assertThat(result.emailVerified()).isFalse();

        var saved = accountRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("qwerty1234");
        assertThat(passwordEncoder.matches("qwerty1234", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_shouldThrowConflictWhenEmailAlreadyExists() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        accountService.register(email, "qwerty1234");

        assertThatThrownBy(() -> accountService.register(email, "anotherPass123"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("email already exists");
    }

    @Test
    void authenticate_shouldReturnAccountWhenCredentialsAreValid() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        accountService.register(email, "qwerty1234");

        AccountDto result = accountService.authenticate(email, "qwerty1234");

        assertThat(result.email()).isEqualTo(normalizeEmail(email));
    }

    @Test
    void authenticate_shouldThrowWhenPasswordIsInvalid() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        accountService.register(email, "qwerty1234");

        assertThatThrownBy(() -> accountService.authenticate(email, "wrongPass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void provisionOAuthAccount_shouldCreateNewAccountIfNotExists() {
        String email = "oauth-" + UUID.randomUUID() + "@mail.com";

        AccountDto result = accountService.provisionOAuthAccount("google", "12345", email, true);

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo(email);
        assertThat(result.emailVerified()).isTrue();

        var saved = accountRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getOauth2Provider()).isEqualTo("google");
        assertThat(saved.getOauth2ProviderId()).isEqualTo("12345");
        assertThat(saved.getPasswordHash()).isNull();
    }

    @Test
    void provisionOAuthAccount_shouldLinkOAuthToExistingEmailAccount() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        AccountDto passwordAccount = accountService.register(email, "qwerty1234");
        AccountDto oauthAccount = accountService.provisionOAuthAccount("github", "999", email, true);
        assertThat(oauthAccount.id()).isEqualTo(passwordAccount.id());
        var saved = accountRepository.findById(passwordAccount.id()).orElseThrow();
        assertThat(saved.getOauth2Provider()).isEqualTo("github");
        assertThat(saved.getOauth2ProviderId()).isEqualTo("999");
        assertThat(saved.getPasswordHash()).isNotNull();
    }

    @Test
    void changePassword_shouldReplaceHashAndAllowNewPassword() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        AccountDto account = accountService.register(email, "qwerty1234");

        accountService.changePassword(account.id(), "newPass12345");

        assertThatThrownBy(() -> accountService.authenticate(account.email(), "qwerty1234"))
                .isInstanceOf(InvalidCredentialsException.class);

        AccountDto afterChange = accountService.authenticate(account.email(), "newPass12345");
        assertThat(afterChange.email()).isEqualTo(normalizeEmail(email));
    }

    @Test
    void changePassword_shouldThrowWhenAccountNotFound() {
        assertThatThrownBy(() -> accountService.changePassword(Long.MAX_VALUE, "newPass12345"))
                .isInstanceOf(InvalidResetTokenException.class)
                .hasMessage("account not found");
    }

    @Test
    void markEmailVerified_shouldSetEmailVerifiedToTrue() {
        String email = "   Test-  " + UUID.randomUUID() + "@mail.com";
        AccountDto account = accountService.register(email, "qwerty1234");
        assertThat(account.emailVerified()).isFalse();

        accountService.markEmailVerified(account.id());

        var saved = accountRepository.findById(account.id()).orElseThrow();
        assertThat(saved.isEmailVerified()).isTrue();
    }

    @Test
    void markEmailVerified_shouldThrowWhenAccountNotFound() {
        assertThatThrownBy(() -> accountService.markEmailVerified(Long.MAX_VALUE))
                .isInstanceOf(InvalidVerificationTokenException.class)
                .hasMessage("account not found");
    }


    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
