/*
 * Copyright (c) 2022 The University of Manchester
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
