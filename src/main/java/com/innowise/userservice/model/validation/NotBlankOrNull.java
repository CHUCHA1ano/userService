package com.innowise.userservice.model.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Jakarta Validation 3.2 made the built-in @NotBlank fail on null as well, but update DTOs
 * treat null as "keep the current value", so optional string fields need this constraint:
 * null passes, and empty/whitespace-only values are rejected.
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotBlankOrNull.NotBlankOrNullValidator.class)
public @interface NotBlankOrNull {

    String message() default "Value cannot be empty";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class NotBlankOrNullValidator implements ConstraintValidator<NotBlankOrNull, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || !value.isBlank();
        }
    }
}
