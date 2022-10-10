/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

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
	 */
	Class<?>[] groups() default {};

	/**
	 * Payload info. Required by validation spec.
	 *
	 * @return Payloads, if any.
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
