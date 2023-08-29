/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.Statement;

import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * Used to document that some DML is expected to generate an ID.
 * <p>
 * <strong>NB:</strong> Do <em>not</em> use with SQLite from 3.43 onwards. The
 * Xerial driver <em>removed</em> support for
 * {@link Statement#getGeneratedKeys()} in version 3.43.0; use a
 * {@code RETURNING} clause instead, and handle via
 * {@link Statement#executeQuery()} instead of
 * {@link Statement#executeUpdate()}. This limitation only applies to that
 * driver.
 *
 * @author Donal Fellows
 */
@Retention(SOURCE)
@Target(FIELD)
@Documented
@UsedInJavadocOnly(Statement.class)
public @interface GeneratesID {
}
