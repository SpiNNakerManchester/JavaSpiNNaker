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

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

/**
 * Validates that a number looks like a UDP port. Always accepts {@code null}.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE_USE })
@Constraint(validatedBy = UDPPort.Validator.class)
public @interface UDPPort {
	/** The minimum ordinary UDP port number. */
	int MIN_UDP_PORT = 1024;

	/** The maximum UDP port number. */
	int MAX_UDP_PORT = 65535;

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
	String message() default "${validatedValue} is a bad UDP port";

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
	 * Validator for {@link UDPPort} constraints. Not intended for direct use.
	 */
	class Validator implements ConstraintValidator<UDPPort, Integer> {
		private static final int MAX_STD_PORT = 32767;

		private boolean acceptZero;

		private int min;

		private int max;

		@Override
		public void initialize(UDPPort annotation) {
			acceptZero = annotation.any();
			min = annotation.system() ? 1 : UDPPort.MIN_UDP_PORT;
			max = annotation.ephemeral() ? UDPPort.MAX_UDP_PORT : MAX_STD_PORT;
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
