/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.compat;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * Validates that an argument is a sane value to pass in a classic spalloc API
 * call. That means "is it a string, a boolean or a number"? Null is not
 * permitted.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE_USE })
@Constraint(validatedBy = { IsSaneValidator.class })
public @interface SaneParameter {
	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "${validatedValue} is a bad spalloc parameter";

	/**
	 * Group of constraints. Required by validation spec.
	 *
	 * @return Constraint groups, if any
	 * @hidden
	 */
	Class<?>[] groups() default {};

	/**
	 * Payload info. Required by validation spec.
	 *
	 * @return Payloads, if any.
	 * @hidden
	 */
	Class<? extends Payload>[] payload() default {};
}

class IsSaneValidator implements ConstraintValidator<SaneParameter, Object> {
	@Override
	public boolean isValid(Object value, ConstraintValidatorContext context) {
		if (value instanceof String) {
			return !((String) value).isBlank();
		} else {
			return (value instanceof Boolean) || (value instanceof Number);
		}
	}
}
