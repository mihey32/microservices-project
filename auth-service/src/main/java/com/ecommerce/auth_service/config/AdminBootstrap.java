package com.ecommerce.auth_service.config;

import com.ecommerce.auth_service.model.Role;
import com.ecommerce.auth_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final AccountService accountService;

    @Value("${app.bootstrap.admin.email:}")
    private String adminEmail;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank()) {
            log.debug("Bootstrap admin not configured (app.bootstrap.admin.email is empty)");
            return;
        }

        accountService.findAccountByEmail(adminEmail)
                .ifPresentOrElse(
                        accountDto -> {
                            accountService.changeRole(accountDto.id(), Role.ROLE_ADMIN);
                            log.info("Bootstrap: promoted existing account to ROLE_ADMIN: accountId={}", accountDto.id());
                        },
                        () -> log.warn("admin is not registered")
                );
    }
}
