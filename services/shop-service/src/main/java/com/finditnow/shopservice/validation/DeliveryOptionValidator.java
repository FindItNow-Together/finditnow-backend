package com.finditnow.shopservice.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class DeliveryOptionValidator implements ConstraintValidator<DeliveryOption, String> {
    private static final Set<String> VALID_OPTIONS = Set.of(
            "NO_DELIVERY",
            "IN_HOUSE_DRIVER",
            "THIRD_PARTY_PARTNER"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotBlank handle null validation
        }
        return VALID_OPTIONS.contains(value);
    }
}

