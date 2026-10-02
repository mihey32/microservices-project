package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.LoginRequest;
import com.ecommerce.auth_service.dto.RegisterRequest;
import com.ecommerce.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AccountDto> register(@RequestBody @Valid RegisterRequest request) {
        log.info("Register attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("login attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.OK)
                .body(authService.login(request));
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(String refreshToken) {
        return null;
    }

    @PostMapping("/logout")
    public void logout() {

    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(String email) {
        return null;
    }


    // /forgot-password, /reset-password, /oauth2/
}
