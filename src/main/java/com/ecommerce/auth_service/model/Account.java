package com.ecommerce.auth_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "accounts",
        indexes = @Index(name = "idx_account_oauth", columnList = "oauth2_provider, oauth2_provider_id"))
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "email_verified", nullable = false)
    @ColumnDefault("false")
    private boolean emailVerified;

    @Column(name = "blocked", nullable = false)
    @ColumnDefault("false")
    private boolean blocked;

    @Column(name = "oauth2_provider")
    private String oauth2Provider;

    @Column(name = "oauth2_provider_id")
    private String oauth2ProviderId;
}
