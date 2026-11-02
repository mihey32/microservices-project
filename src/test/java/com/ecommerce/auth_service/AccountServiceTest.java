package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.exceptions.InvalidCredentialsException;
import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.repository.AccountRepository;
import com.ecommerce.auth_service.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

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
        AccountDto result = accountService.register("  Test@Mail.COM  ", "qwerty1234");

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("test@mail.com");
        assertThat(result.role()).isEqualTo(Role.ROLE_USER);
        assertThat(result.emailVerified()).isFalse();

        var saved = accountRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("qwerty1234");
        assertThat(passwordEncoder.matches("qwerty1234", saved.getPasswordHash())).isTrue();
    }

    @Test
    void register_shouldThrowConflictWhenEmailAlreadyExists() {
        accountService.register("duplicate@mail.com", "qwerty1234");

        assertThatThrownBy(() -> accountService.register("duplicate@mail.com", "anotherPass123"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("email already exists");
    }

    @Test
    void authenticate_shouldReturnAccountWhenCredentialsAreValid() {
        accountService.register("auth@mail.com", "qwerty1234");

        AccountDto result = accountService.authenticate("AUTH@mail.com", "qwerty1234");

        assertThat(result.email()).isEqualTo("auth@mail.com");
    }

    @Test
    void authenticate_shouldThrowWhenPasswordIsInvalid() {
        accountService.register("auth2@mail.com", "qwerty1234");

        assertThatThrownBy(() -> accountService.authenticate("auth2@mail.com", "wrongPass"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void provisionOAuthAccount_shouldCreateNewAccountIfNotExists() {
        AccountDto result = accountService.provisionOAuthAccount("google", "12345", "oauth@mail.com", true);

        assertThat(result.id()).isNotNull();
        assertThat(result.email()).isEqualTo("oauth@mail.com");
        assertThat(result.emailVerified()).isTrue();

        var saved = accountRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getOauth2Provider()).isEqualTo("google");
        assertThat(saved.getOauth2ProviderId()).isEqualTo("12345");
        assertThat(saved.getPasswordHash()).isNull();
    }

    @Test
    void provisionOAuthAccount_shouldLinkOAuthToExistingEmailAccount() {
        AccountDto passwordAccount = accountService.register("link@mail.com", "qwerty1234");
        AccountDto oauthAccount = accountService.provisionOAuthAccount("github", "999", "link@mail.com", true);
        assertThat(oauthAccount.id()).isEqualTo(passwordAccount.id());
        var saved = accountRepository.findById(passwordAccount.id()).orElseThrow();
        assertThat(saved.getOauth2Provider()).isEqualTo("github");
        assertThat(saved.getOauth2ProviderId()).isEqualTo("999");
        assertThat(saved.getPasswordHash()).isNotNull();
    }
}
