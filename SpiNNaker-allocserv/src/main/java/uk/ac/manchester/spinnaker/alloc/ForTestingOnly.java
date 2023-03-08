/*
 * Copyright (c) 2022 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.StackWalker.StackFrame;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;

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
		/**
		 * The <em>name</em> of the class used to annotate tests that can access
		 * bean test APIs.
		 */
		private static final String SPRING_BOOT_TEST =
				"org.springframework.boot.test.context.SpringBootTest";

		private Utils() {
		}

		/**
		 * A simple test for whether there are classes annotated with
		 * {@code @SpringBootTest} on the stack at the point it is called.
		 * Moderately expensive, but it only guards stuff that should be used on
		 * test paths, so that isn't important.
		 *
		 * @throws Error
		 *             if not called from the right place; it's a serious
		 *             security failure and wrong programming.
		 */
		// No link to doc; built in code that doesn't see test classes
		public static void checkForTestClassOnStack() {
			if (!StackWalker.getInstance(RETAIN_CLASS_REFERENCE)
					.walk(s -> s.map(StackFrame::getDeclaringClass) //
							.map(Class::getAnnotations) //
							.flatMap(Arrays::stream) //
							.map(Annotation::annotationType) //
							.map(Class::getName) //
							.anyMatch(SPRING_BOOT_TEST::equals))) {
				throw new Error("test-only code called from non-test context");
			}
		}
	}
}
