package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.AuthResponse;
import com.ecommerce.auth_service.dto.ErrorResponseDto;
import com.ecommerce.auth_service.dto.request.*;
import com.ecommerce.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Auth", description = "Регистрация, вход, токены, восстановление пароля")
public class AuthController {

    private static final String ERR = "application/json";

    private final AuthService authService;

    @Operation(summary = "Регистрация по email/паролю", description = "Создаёт аккаунт и публикует событие `user.created` в Kafka")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Аккаунт создан"),
            @ApiResponse(responseCode = "409", description = "Email занят",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/register")
    public ResponseEntity<AccountDto> register(@RequestBody @Valid RegisterRequest request) {
        log.debug("Register attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @Operation(summary = "Логин по email/паролю")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access + refresh токены"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации (email/password)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Неверные email или пароль",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "403", description = "Email не подтверждён или аккаунт заблокирован",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        log.debug("Login attempt for email={}", request.email());
        return ResponseEntity.status(HttpStatus.OK)
                .body(authService.login(request));
    }

    @Operation(summary = "Обновление access-токена по refresh-токену")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новая пара токенов"),
            @ApiResponse(responseCode = "400", description = "Пустой refreshToken",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Refresh-токен недействителен, истёк или уже использован",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid RefreshRequest refreshRequest) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(authService.refresh(refreshRequest));
    }

    @Operation(summary = "Logout — отзыв refresh-токена",
            description = "Идемпотентен: повторный вызов с уже отозванным токеном вернёт 204")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Токен отозван (или уже был отозван)"),
            @ApiResponse(responseCode = "400", description = "Пустой refreshToken",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshRequest refreshRequest) {
        log.debug("Logout attempt");
        authService.logout(refreshRequest);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(summary = "Подтверждение email по токену из письма")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Email подтверждён"),
            @ApiResponse(responseCode = "400", description = "Пустой token",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Токен недействителен, истёк или уже использован",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        log.debug("Verify email attempt");
        authService.verifyEmail(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(summary = "Запрос на сброс пароля",
            description = """
                       Публикует событие `password.reset-requested` в Kafka.
                       Всегда возвращает 204 — независимо от того, существует ли email (защита от enumeration).
                       """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Запрос принят (email может и не существовать)"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации email",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        log.debug("Forgot password attempt");
        authService.forgotPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(summary = "Сброс пароля по токену из письма")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пароль изменён, все refresh-токены аккаунта отозваны"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустой token, пароль короче 8 или длиннее 72)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Токен недействителен, истёк или уже использован",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        log.debug("Reset password attempt");
        authService.resetPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build();
    }

    @Operation(summary = "Смена пароля (требует JWT)",
            description = "После успешной смены все refresh-токены аккаунта отзываются",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Пароль изменён, refresh-токены отозваны"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации (пустые поля, пароль короче 8 или длиннее 72)",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Неверный старый пароль, пароль совпадает со старым или отсутствует JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
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
