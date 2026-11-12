package com.ecommerce.auth_service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordSizeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordSize {
    int min() default 0;

    int max() default 72;

    String message() default "password must be between {min} and {max} bytes (UTF-8)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
