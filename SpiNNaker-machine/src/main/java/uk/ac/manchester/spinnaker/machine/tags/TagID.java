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
 * Validates that a number looks like a tag identifier. Always accepts
 * {@code null}.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER, TYPE_USE })
@Constraint(validatedBy = TagIDValidator.class)
public @interface TagID {
	/**
	 * Whether to allow the SC&amp;MP-dedicated tag.
	 *
	 * @return Whether 0 is allowed.
	 */
	boolean scamp() default false;

	/**
	 * Whether to also allow ephemeral tags.
	 *
	 * @return Whether 8-15 are allowed
	 */
	boolean ephemeral() default false;

	/**
	 * Message on constraint violated.
	 *
	 * @return Message
	 */
	String message() default "${validatedValue} is a bad tag identifier";

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

class TagIDValidator implements ConstraintValidator<TagID, Integer> {
	private static final int MAX_TAG = 7;

	private static final int MAX_EPHEMERAL = 15;

	private int min;

	private int max;

	@Override
	public void initialize(TagID annotation) {
		min = annotation.scamp() ? 0 : 1;
		max = annotation.ephemeral() ? MAX_EPHEMERAL : MAX_TAG;
	}

	@Override
	public boolean isValid(Integer value, ConstraintValidatorContext context) {
		if (isNull(value)) {
			return true;
		}
		return (value >= min) && (value <= max);
	}
}
