package com.finditnow.shopservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LatitudeValidator.class)
@Documented
public @interface Latitude {
    String message() default "Latitude must be between -90 and 90";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

