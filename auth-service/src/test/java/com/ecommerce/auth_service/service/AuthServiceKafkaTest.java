package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.AbstractIntegrationTest;
import com.ecommerce.avro.EmailNotificationEvent;
import com.ecommerce.avro.UserCreatedEvent;
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

    @Value("${spring.kafka.producer.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    private List<UserCreatedEvent> readUserCreatedFor(String email, int expectedCount, Duration timeout) {
        return KafkaTestUtils.readMessages(
                bootstrapServers, schemaRegistryUrl, "user.created", UserCreatedEvent.class,
                e -> e.getEmail().equals(email), expectedCount, timeout);
    }

    private List<EmailNotificationEvent> readPasswordResetFor(String email, int expectedCount, Duration timeout) {
        return KafkaTestUtils.readMessages(
                bootstrapServers, schemaRegistryUrl, "password.reset-requested", EmailNotificationEvent.class,
                e -> e.getEmail().equals(email), expectedCount, timeout);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    @Test
    void shouldPublishUserCreatedEventWhenUserRegisters() {
        String email = uniqueEmail("kafka-happy");

        var account = authService.register(new RegisterRequest(email, "Password123!"));

        var events = readUserCreatedFor(email, 1, Duration.ofSeconds(30));

        assertThat(events).hasSize(1);
        UserCreatedEvent event = events.get(0);
        assertThat(event.getUserId()).isEqualTo(account.id());
        assertThat(event.getEmail()).isEqualTo(email);
        assertThat(event.getCreatedAt()).isPositive();
    }

    @Test
    void shouldNotPublishSecondEventWhenRegistrationConflicts() {
        String email = uniqueEmail("kafka-dup");
        authService.register(new RegisterRequest(email, "Password123!"));   // публикует 1 событие

        assertThatThrownBy(() -> authService.register(new RegisterRequest(email, "Password123!")))
                .isInstanceOf(ConflictException.class);

        var events = readUserCreatedFor(email, 2, Duration.ofSeconds(30));

        assertThat(events).hasSize(1);
    }

    @Test
    void shouldPublishEmailNotificationEventOnForgotPassword() {
        String email = uniqueEmail("kafka-reset");
        authService.register(new RegisterRequest(email, "Password123!"));

        authService.forgotPassword(new ForgotPasswordRequest(email));

        var events = readPasswordResetFor(email, 1, Duration.ofSeconds(30));

        assertThat(events).hasSize(1);
        EmailNotificationEvent event = events.get(0);
        assertThat(event.getEmail()).isEqualTo(email);
        assertThat(event.getResetToken()).isNotBlank();
        assertThat(event.getCreatedAt()).isPositive();
    }

    @Test
    void shouldNotPublishEventOnForgotPasswordForUnknownEmail() {
        String email = uniqueEmail("kafka-unknown");

        authService.forgotPassword(new ForgotPasswordRequest(email));

        var events = readPasswordResetFor(email, 1, Duration.ofSeconds(30));

        assertThat(events).isEmpty();
    }

    @Test
    void shouldPublishEmailVerificationEventWhenUserRegisters() {
        String email = uniqueEmail("kafka-verify");

        authService.register(new RegisterRequest(email, "Password123!"));

        var events = KafkaTestUtils.readMessages(
                bootstrapServers, schemaRegistryUrl, "email.verification-requested", EmailNotificationEvent.class,
                e -> e.getEmail().equals(email), 1, Duration.ofSeconds(30));

        assertThat(events).hasSize(1);
        EmailNotificationEvent event = events.get(0);
        assertThat(event.getEmail()).isEqualTo(email);
        assertThat(event.getResetToken()).isNotBlank();
    }
}
