package com.devsu.customers.infrastructure.adapter.in.rest.dto;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Limits the UTF-8 encoded length of a string. Needed for passwords because BCrypt only accepts up to 72 bytes,
 * and characters such as accents or emojis take more than one byte.
 */
@Documented
@Constraint(validatedBy = MaxBytes.Validator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxBytes {

	int value();

	String message() default "must not exceed {value} bytes when UTF-8 encoded";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	class Validator implements ConstraintValidator<MaxBytes, String> {

		private int maxBytes;

		@Override
		public void initialize(MaxBytes annotation) {
			this.maxBytes = annotation.value();
		}

		@Override
		public boolean isValid(String value, ConstraintValidatorContext context) {
			return value == null || value.getBytes(StandardCharsets.UTF_8).length <= maxBytes;
		}
	}
}
