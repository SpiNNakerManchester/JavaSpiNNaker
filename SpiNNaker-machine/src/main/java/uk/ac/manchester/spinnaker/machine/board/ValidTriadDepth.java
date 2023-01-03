/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine.board;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * Validates that a machine's depth is in a sane range. Note that only machines
 * with a single board typically have a depth other than 3, and the current
 * implementation never uses a depth of 2.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE_USE })
@Positive
@Max(Limits.MAX_TRIAD_Z + 1)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
public @interface ValidTriadDepth {
	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "triad depth (${validatedValue}) "
			+ "must be at least 1 and no more than 3";

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
