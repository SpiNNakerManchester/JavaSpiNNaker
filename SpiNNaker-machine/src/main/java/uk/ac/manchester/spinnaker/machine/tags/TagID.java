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
