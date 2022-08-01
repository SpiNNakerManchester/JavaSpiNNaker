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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks that the given class is used for Javadoc generation only, but needs to
 * be referenced for Checkstyle. With this, the classes in question are
 * genuinely used, but only at the source level; no runtime code is generated.
 *
 * @author Donal Fellows
 */
@Retention(SOURCE)
@Target({ TYPE, METHOD, CONSTRUCTOR, FIELD })
public @interface UsedInJavadocOnly {
	/**
	 * Class or classes just used by the annotated thing in Javadoc generation.
	 * Put in with this so Checkstyle doesn't think the class is unused.
	 *
	 * @return classes
	 */
	Class<?>[] value();
}
