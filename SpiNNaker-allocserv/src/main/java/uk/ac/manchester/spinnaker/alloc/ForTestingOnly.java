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
package uk.ac.manchester.spinnaker.alloc;

import static java.lang.Thread.currentThread;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;
import static java.util.Arrays.stream;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a type or method that only exists for testing purposes. Do not use for
 * any other reason! Methods tagged with this <em>should</em> also be tagged
 * {@link Deprecated @Deprecated}; classes/interfaces tagged with this need not.
 *
 * @author Donal Fellows
 */
@Documented
@Retention(CLASS)
@Target({ TYPE, METHOD })
public @interface ForTestingOnly {
	/** Utilities for checking the promises relating to the annotation. */
	abstract class Utils {
		private Utils() {
		}

		/**
		 * A simple test for whether there are test classes on the stack at the
		 * point it is called. Moderately expensive, but it only guards stuff
		 * that should be used on test paths, so that isn't important.
		 *
		 * @throws Error
		 *             if not called from the right place; it's a serious
		 *             security failure and wrong programming.
		 */
		public static void checkForTestClassOnStack() {
			// TODO change to java.lang.StackWalker in Java 11 onwards
			if (!stream(currentThread().getStackTrace())
					.anyMatch(e -> e.getClassName().endsWith("Test"))) {
				throw new Error("test-only code called from non-test context");
			}
		}
	}
}
