package com.ecommerce.auth_service.service;

import com.ecommerce.auth_service.repository.PasswordResetTokenRepository;
import com.ecommerce.auth_service.util.TokenHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
public class PasswordResetService {

    private final TokenHasher tokenHasher;
    private final PasswordResetTokenRepository repository;
    private final Duration resetTokenExpiration;

    public PasswordResetService(TokenHasher tokenHasher,
                                PasswordResetTokenRepository repository,
                                @Value("${jwt.password-reset.expiration}") Duration resetTokenExpiration) {
        this.tokenHasher = tokenHasher;
        this.repository = repository;
        this.resetTokenExpiration = resetTokenExpiration;
    }


}
