package com.ecommerce.auth_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class PasswordSizeValidator  implements ConstraintValidator<PasswordSize, String> {

    private int min;
    private int max;

    @Override
    public void initialize(PasswordSize constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= min && bytes <= max;
    }
}
