package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.AbstractIntegrationTest;
import com.ecommerce.auth_service.dto.EmailNotificationEvent;
import com.ecommerce.auth_service.dto.UserCreatedEvent;
import com.ecommerce.auth_service.dto.request.ForgotPasswordRequest;
import com.ecommerce.auth_service.dto.request.RegisterRequest;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.util.KafkaTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class AuthServiceKafkaTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private List<UserCreatedEvent> readUserCreatedFor(String email, int expectedCount, Duration timeout) {
        return KafkaTestUtils.readMessages(
                bootstrapServers, "user.created", UserCreatedEvent.class,
                e -> e.email().equals(email), expectedCount, timeout);
    }

    private List<EmailNotificationEvent> readPasswordResetFor(String email, int expectedCount, Duration timeout) {
        return KafkaTestUtils.readMessages(
                bootstrapServers, "password.reset-requested", EmailNotificationEvent.class,
                e -> e.email().equals(email), expectedCount, timeout);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void shouldPublishUserCreatedEventWhenUserRegisters() {
        String email = uniqueEmail("kafka-happy");

        var account = authService.register(new RegisterRequest(email, "Password123!"));

        var events = readUserCreatedFor(email, 1, Duration.ofSeconds(10));

        assertThat(events).hasSize(1);
        UserCreatedEvent event = events.get(0);
        assertThat(event.userId()).isEqualTo(account.id());
        assertThat(event.email()).isEqualTo(email);
        assertThat(event.verificationToken()).isNotBlank();
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void shouldNotPublishSecondEventWhenRegistrationConflicts() {
        String email = uniqueEmail("kafka-dup");
        authService.register(new RegisterRequest(email, "Password123!"));   // публикует 1 событие

        assertThatThrownBy(() -> authService.register(new RegisterRequest(email, "Password123!")))
                .isInstanceOf(ConflictException.class);

        var events = readUserCreatedFor(email, 2, Duration.ofSeconds(5));

        assertThat(events).hasSize(1);
    }

    @Test
    void shouldPublishEmailNotificationEventOnForgotPassword() {
        String email = uniqueEmail("kafka-reset");
        authService.register(new RegisterRequest(email, "Password123!"));

        authService.forgotPassword(new ForgotPasswordRequest(email));

        var events = readPasswordResetFor(email, 1, Duration.ofSeconds(10));

        assertThat(events).hasSize(1);
        EmailNotificationEvent event = events.get(0);
        assertThat(event.email()).isEqualTo(email);
        assertThat(event.resetToken()).isNotBlank();
        assertThat(event.createdAt()).isNotNull();
    }

    @Test
    void shouldNotPublishEventOnForgotPasswordForUnknownEmail() {
        String email = uniqueEmail("kafka-unknown");

        authService.forgotPassword(new ForgotPasswordRequest(email));

        var events = readPasswordResetFor(email, 1, Duration.ofSeconds(3));

        assertThat(events).isEmpty();
    }
}
