/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.data_spec;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is used to indicate which methods should be used to implement
 * DSE operations. Methods annotated with this <em>must</em> be {@code public},
 * <em>must</em> take no arguments, and <em>must</em> have a return type of
 * {@code void}, {@code int}, or {@link Integer}. The operation calling
 * mechanism treats methods that don't return a value as if they returned zero.
 * The only checked exception that may be thrown is a
 * {@link DataSpecificationException}.
 *
 * @author Donal Fellows
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
public @interface Operation {
	/**
	 * Describes what Data Specification operation is implemented by the method
	 * the annotation is on. This <em>must not</em> be {@code null}.
	 *
	 * @return The DSE operation descriptor.
	 */
	Commands value();
}
