package com.ecommerce.auth_service.controller;

import com.ecommerce.auth_service.dto.AccountDto;
import com.ecommerce.auth_service.dto.ErrorResponseDto;
import com.ecommerce.auth_service.dto.request.ChangeBlockStatusRequest;
import com.ecommerce.auth_service.dto.request.ChangeRoleRequest;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.service.AccountService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/accounts")
@Tag(name = "Admin", description = "Управление ролями и блокировкой аккаунтов (ROLE_ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private static final String ERR = "application/json";

    private final AccountService accountService;

    @Operation(summary = "Сменить роль аккаунта (требует ROLE_ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Роль изменена"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Нет JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "403", description = "Нет ROLE_ADMIN или попытка изменить свою роль",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<AccountDto> changeRole(@PathVariable Long id,
                                                 @RequestBody @Valid ChangeRoleRequest request,
                                                 Authentication authentication) {
        AccountDto self = accountService.findAccountByEmail(authentication.getName())
                .orElseThrow(() -> new ConflictException("current account not found"));
        if (self.id().equals(id)) {
            log.warn("Attempt to change own role by admin: accountId={}", id);
            throw new ConflictException("cannot change your own role");
        }

        log.debug("Admin changes role: targetId={}, newRole={}", id, request.role());
        return ResponseEntity.status(HttpStatus.OK).body(accountService.changeRole(id, request.role()));
    }

    @Operation(summary = "Заблокировать/разблокировать аккаунт (требует ROLE_ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус изменён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "401", description = "Нет JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "403", description = "Нет ROLE_ADMIN или попытка заблокировать себя",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR)),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorResponseDto.class), mediaType = ERR))
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/block")
    public ResponseEntity<AccountDto> changeBlockStatus(@PathVariable Long id,
                                                        @RequestBody @Valid ChangeBlockStatusRequest request,
                                                        Authentication authentication) {
        AccountDto self = accountService.findAccountByEmail(authentication.getName())
                .orElseThrow(() -> new ConflictException("current account not found"));
        if (self.id().equals(id)) {
            log.warn("Attempt to block self by admin: accountId={}", id);
            throw new ConflictException("cannot block yourself");
        }

        log.debug("Admin changes block status: targetId={}, blocked={}", id, request.blocked());
        return ResponseEntity.status(HttpStatus.OK).body(accountService.setBlocked(id, request.blocked()));

    }
}
