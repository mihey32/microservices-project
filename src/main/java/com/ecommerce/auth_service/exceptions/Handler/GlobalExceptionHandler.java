package com.ecommerce.auth_service.exceptions.Handler;

import com.ecommerce.auth_service.dto.ErrorResponseDto;
import com.ecommerce.auth_service.exceptions.AccountBlockedException;
import com.ecommerce.auth_service.exceptions.ConflictException;
import com.ecommerce.auth_service.exceptions.EmailNotConfirmedException;
import com.ecommerce.auth_service.exceptions.InvalidCredentialsException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {
            EmailNotConfirmedException.class,
            AccountBlockedException.class
    })
    public ResponseEntity<ErrorResponseDto> handleForbiddenException(Exception e) {
        var errorDto = new ErrorResponseDto(
                "Forbidden request",
                e.getMessage(),
                Instant.now()
        );

        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(errorDto);
    }

    @ExceptionHandler(value = {
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception e) {
        String error = e.getMessage();

        if (e instanceof MethodArgumentNotValidException ex) {
            error = ex.getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        }

        var errorDto = new ErrorResponseDto(
                "Bad request",
                error,
                Instant.now()
        );

        log.warn("Bad request: {}", error);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorDto);
    }


    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleInvalidCredentialsException(InvalidCredentialsException e) {
        var errorDto = new ErrorResponseDto(
                "Invalid credentials",
                e.getMessage(),
                Instant.now()
        );

        log.warn("Invalid credentials attempt");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorDto);
    }

    @ExceptionHandler
    public ResponseEntity<ErrorResponseDto> handleConflictException(ConflictException e) {
        var errorDto = new ErrorResponseDto(
                "Conflict",
                e.getMessage(),
                Instant.now()
        );

        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(Exception e) {
        var errorDto = new ErrorResponseDto(
                "Internal server error",
                "Please contact support",
                Instant.now()
        );

        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorDto);
    }
}
