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
package uk.ac.manchester.spinnaker.machine.tags;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Objects.isNull;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validates that a number looks like a TCP port. Always accepts {@code null}.
 *
 * @author Donal Fellows
 * @see UDPPort
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE_USE })
@Constraint(validatedBy = TCPPort.Validator.class)
public @interface TCPPort {
	/** The minimum ordinary TCP port number. */
	int MIN_TCP_PORT = 1024;

	/** The maximum TCP port number. */
	int MAX_TCP_PORT = 65535;

	/**
	 * Whether to allow the "any" port. Note that this is not the same as
	 * allowing any other port; it's more that it isn't bound to a specific
	 * port.
	 *
	 * @return Whether 0 is allowed.
	 */
	boolean any() default false;

	/**
	 * Whether to allow system ports.
	 *
	 * @return Whether 1-1023 is allowed.
	 */
	boolean system() default false;

	/**
	 * Whether to allow ephemeral ports.
	 *
	 * @return Whether 32768-65535 is allowed
	 */
	boolean ephemeral() default true;

	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "${validatedValue} is a bad TCP port";

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

	class Validator implements ConstraintValidator<TCPPort, Integer> {
		private static final int MAX_STD_PORT = 32767;

		private boolean acceptZero;

		private int min;

		private int max;

		@Override
		public void initialize(TCPPort annotation) {
			acceptZero = annotation.any();
			min = annotation.system() ? 1 : MIN_TCP_PORT;
			max = annotation.ephemeral() ? MAX_TCP_PORT : MAX_STD_PORT;
		}

		@Override
		public boolean isValid(Integer value,
				ConstraintValidatorContext context) {
			if (isNull(value)) {
				return true;
			}
			if (value == 0) {
				return acceptZero;
			}
			return (value >= min) && (value <= max);
		}
	}
}
