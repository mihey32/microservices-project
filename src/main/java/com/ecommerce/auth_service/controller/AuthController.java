package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
        log.debug("Register attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        log.debug("Login attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.OK)
                .body(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest refreshRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(authService.refresh(refreshRequest));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest refreshRequest) {
        log.debug("Logout attempt");
        authService.logout(refreshRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        log.debug("Verify email attempt");
        authService.verifyEmail(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        log.debug("Forgot password attempt");
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        log.debug("Reset password attempt");
        authService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest,
                                               Authentication authentication) {
        log.debug("Change password attempt");
        String email = authentication.getName();
        authService.changePassword(changePasswordRequest, email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

}
