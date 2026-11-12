package com.ecommerce.auth_service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.HashSet;
import java.util.Set;

public class AllowedPasswordCharsValidator implements ConstraintValidator<AllowedPasswordChars, String> {

    private Set<Character> allowedChars;

    @Override
    public void initialize(AllowedPasswordChars constraintAnnotation) {
        this.allowedChars = new HashSet<>(constraintAnnotation.allowed().length());
        for (char c : constraintAnnotation.allowed().toCharArray()) {
            allowedChars.add(c);
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!allowedChars.contains(c)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                "password contains forbidden character : '" + c + "'")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
