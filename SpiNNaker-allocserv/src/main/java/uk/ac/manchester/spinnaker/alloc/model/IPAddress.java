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
package uk.ac.manchester.spinnaker.alloc.model;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validates that a string looks like an IP address.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({
	METHOD, FIELD, PARAMETER, TYPE_USE
})
@Constraint(validatedBy = IPAddressValidator.class)
public @interface IPAddress {
	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "${validatedValue} is a bad IPv4 address";

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

class IPAddressValidator implements ConstraintValidator<IPAddress, String> {
	private Pattern pattern;

	@Override
	public void initialize(IPAddress annotation) {
		if (isNull(pattern)) {
			pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+[.]\\d+$");
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return nonNull(value) && pattern.matcher(value).matches();
	}
}
