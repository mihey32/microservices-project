package com.ecommerce.auth_service;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.service.AccountService;
import com.ecommerce.auth_service.service.AuthService;
import com.ecommerce.auth_service.service.EmailVerificationService;
import com.ecommerce.auth_service.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Transactional
public class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AuthService authService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private PasswordResetService passwordResetService;

    private WebTestClient client;


    @BeforeEach
    void setUp() {
        client = MockMvcWebTestClient.bindTo(mockMvc).build();
        Set<String> keys = redisTemplate.keys("rl:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
    }

    private String uniqueEmail() {
        return "AuthControllerTest-" + UUID.randomUUID() + "@mail.com";
    }

    private AccountDto registerVerified(String email) {
        AccountDto a = accountService.register(email, "qwerty1234");
        accountService.markEmailVerified(a.id());
        return accountService.findAccountById(a.id()).orElseThrow();
    }

    private String bearer(AccountDto account) {
        return "Bearer " + authService.login(
                new LoginRequest(account.email(), "qwerty1234")).accessToken();
    }

    @Test
    void register_valid_returns201WithAccount() {
        String email = uniqueEmail();

        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(email, "Password123!"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.email").isEqualTo(email.trim().toLowerCase(Locale.ROOT))
                .jsonPath("$.role").isEqualTo("ROLE_USER")
                .jsonPath("$.emailVerified").isEqualTo(false);
    }

    @Test
    void register_invalidEmail_returns400() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest("not-an-email", "Password123!"))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().jsonPath("$.message").isEqualTo("Bad request");
    }

    @Test
    void register_duplicate_returns409() {
        String email = uniqueEmail();
        accountService.register(email, "qwerty1234");

        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(email, "Password123!"))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.message").isEqualTo("Conflict");
    }

    @Test
    void login_valid_returns200WithTokens() {
        AccountDto a = registerVerified(uniqueEmail());

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(a.email(), "qwerty1234"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty()
                .jsonPath("$.account.email").isEqualTo(a.email());
    }

    @Test
    void login_wrongPassword_returns401() {
        AccountDto a = registerVerified(uniqueEmail());

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(a.email(), "wrongPass"))
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.message").isEqualTo("Invalid credentials");
    }

    @Test
    void login_unverified_returns403() {
        AccountDto a = accountService.register(uniqueEmail(), "qwerty1234");

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest(a.email(), "qwerty1234"))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody().jsonPath("$.message").isEqualTo("Forbidden request");
    }

    @Test
    void refresh_valid_returns200NewTokens() {
        AccountDto a = registerVerified(uniqueEmail());
        String refresh = authService.login(new LoginRequest(a.email(), "qwerty1234")).refreshToken();

        client.post().uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RefreshRequest(refresh))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isNotEmpty()
                .jsonPath("$.refreshToken").isNotEmpty();
    }

    @Test
    void refresh_invalid_returns401() {
        client.post().uri("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RefreshRequest(UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logout_valid_returns204() {
        AccountDto a = registerVerified(uniqueEmail());
        String refresh = authService.login(new LoginRequest(a.email(), "qwerty1234")).refreshToken();

        client.post().uri("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RefreshRequest(refresh))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void verifyEmail_valid_returns204() {
        AccountDto a = accountService.register(uniqueEmail(), "qwerty1234");
        String raw = emailVerificationService.generateVerificationToken(a);

        client.post().uri("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new VerifyEmailRequest(raw))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void verifyEmail_invalid_returns401() {
        client.post().uri("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new VerifyEmailRequest(UUID.randomUUID().toString()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void forgotPassword_returns204EvenForUnknownEmail() {
        client.post().uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ForgotPasswordRequest(uniqueEmail()))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void forgotPassword_invalidEmail_returns400() {
        client.post().uri("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ForgotPasswordRequest("not-an-email"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void resetPassword_valid_returns204() {
        AccountDto a = registerVerified(uniqueEmail());
        String raw = passwordResetService.generateResetToken(a);

        client.post().uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ResetPasswordRequest(raw, "newPass12345"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void resetPassword_invalid_returns401() {
        client.post().uri("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ResetPasswordRequest(UUID.randomUUID().toString(), "newPass12345"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void changePassword_withJwt_returns204() {
        AccountDto a = registerVerified(uniqueEmail());

        client.post().uri("/auth/change-password")
                .header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangePasswordRequest("qwerty1234", "newPass12345"))
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void changePassword_withoutJwt_returns401() {
        client.post().uri("/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangePasswordRequest("qwerty1234", "newPass12345"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void changePassword_wrongOld_returns401() {
        AccountDto a = registerVerified(uniqueEmail());

        client.post().uri("/auth/change-password")
                .header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangePasswordRequest("wrongOld", "newPass12345"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void changePassword_invalidBody_returns400() {
        AccountDto a = registerVerified(uniqueEmail());

        client.post().uri("/auth/change-password")
                .header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ChangePasswordRequest("", "short"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void unsupportedContentType_returns415() {
        client.post().uri("/auth/login")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("not-json")
                .exchange()
                .expectStatus().isEqualTo(415);
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-valid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongMethod_returns405() {
        client.get().uri("/auth/register")
                .exchange()
                .expectStatus().isEqualTo(405);
    }


    @Test
    void register_cyrillicPassword_returns400() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(uniqueEmail(), "Пароль1234"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_passwordOver72Bytes_returns400() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(uniqueEmail(), "a".repeat(73)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_cyrillicPasswordOver72Bytes_returns400() {
        // 40 × 'П' = 80 байт UTF-8 (каждый 2 байта) > 72
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(uniqueEmail(), "П".repeat(40)))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_forbiddenSpecialChar_returns400() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(uniqueEmail(), "Password123<"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void register_validPassword_returns201() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new RegisterRequest(uniqueEmail(), "Password123!"))
                .exchange()
                .expectStatus().isCreated();
    }

}
