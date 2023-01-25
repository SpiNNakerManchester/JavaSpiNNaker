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
package uk.ac.manchester.spinnaker.utils.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.isNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validates that a string looks like an IP address. A string is considered to
 * be a valid IP address if it looks like a dotted quad.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({
	METHOD, FIELD, PARAMETER, TYPE_USE
})
@Constraint(validatedBy = IPAddress.Validator.class)
public @interface IPAddress {
	/**
	 * Whether {@code null} is allowed. It defaults to being disallowed.
	 *
	 * @return Whether to accept {@code null}.
	 */
	boolean nullOK() default false;

	/**
	 * Whether the empty string is allowed. It defaults to being disallowed.
	 *
	 * @return Whether to accept the empty string.
	 */
	boolean emptyOK() default false;

	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "'${validatedValue}' is a bad IPv4 address";

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

	/**
	 * Validator for {@link IPAddress} constraints. Not intended for direct use.
	 */
	class Validator implements ConstraintValidator<IPAddress, String> {
		private Pattern pattern;

		private boolean emptyOK;

		private boolean nullOK;

		@Override
		public void initialize(IPAddress annotation) {
			if (isNull(pattern)) {
				pattern = Pattern.compile("^\\d+[.]\\d+[.]\\d+[.]\\d+$");
			}
			emptyOK = annotation.emptyOK();
			nullOK = annotation.nullOK();
		}

		@Override
		public boolean isValid(String value,
				ConstraintValidatorContext context) {
			if (isNull(value)) {
				return nullOK;
			} else if (value.isEmpty()) {
				return emptyOK;
			}
			if (!pattern.matcher(value).matches()) {
				return false;
			}
			// Cheap checks succeeded; use the real parser now!
			try {
				InetAddress.getByName(value);
				return true;
			} catch (UnknownHostException e) {
				return false;
			}
		}
	}
}
