/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

	/**
	 * Validator for {@link IPAddress} constraints. Not intended for direct use.
	 *
	 * @hidden
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
