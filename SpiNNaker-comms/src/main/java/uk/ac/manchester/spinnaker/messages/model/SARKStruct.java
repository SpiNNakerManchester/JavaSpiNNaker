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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A structure that has an equivalent in a structure in SARK (or SCAMP, or the
 * BMP).
 *
 * @author Donal Fellows
 */
@Documented
@Retention(SOURCE)
@Target(TYPE)
public @interface SARKStruct {
	/**
	 * @return The name of the struct in SARK.
	 */
	String value();

	/**
	 * @return Which API contains the structure.
	 */
	API api() default API.SARK;

	/**
	 * The supported APIs for mapped structures.
	 *
	 * @author Donal Fellows
	 */
	enum API {
		/** This identifies a structure defined by SARK. */
		SARK,
		/** This identifies a structure defined by SC&amp;MP. */
		SCAMP,
		/** This identifies a structure defined by spin1_api. */
		SPIN1API,
		/** This identifies a structure defined by the BMP. */
		BMP
	}
}
