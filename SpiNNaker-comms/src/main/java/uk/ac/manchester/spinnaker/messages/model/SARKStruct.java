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
