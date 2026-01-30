package com.finditnow.shopservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DeliveryOptionValidator.class)
@Documented
public @interface DeliveryOption {
    String message() default "Delivery option must be one of: NO_DELIVERY, IN_HOUSE_DRIVER, THIRD_PARTY_PARTNER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

