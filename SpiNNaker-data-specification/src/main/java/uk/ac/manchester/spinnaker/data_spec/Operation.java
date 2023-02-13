/*
 * Copyright (c) 2018-2023 The University of Manchester
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
