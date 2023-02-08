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
package uk.ac.manchester.spinnaker.alloc.db;

/**
 * Flags in SQLite that the driver doesn't name for us.
 */
public interface SQLiteFlags {
	/**
	 * Flag direct from SQLite. Used to mark a function as ineligible for use
	 * other than by written directly in an SQL query.
	 *
	 * <blockquote>The {@code SQLITE_DIRECTONLY} flag means that the function
	 * may only be invoked from top-level SQL, and cannot be used in
	 * <em>VIEW</em>s or <em>TRIGGER</em>s nor in schema structures such as
	 * <em>CHECK</em> constraints, <em>DEFAULT</em> clauses, expression indexes,
	 * partial indexes, or generated columns. The {@code SQLITE_DIRECTONLY}
	 * flags is a security feature which is recommended for all
	 * application-defined SQL functions, and especially for functions that have
	 * side-effects or that could potentially leak sensitive information.
	 * </blockquote>
	 * <p>
	 * Note that the password-related functions we used to install were
	 * definitely examples of functions that are only usable directly. (They
	 * were removed for performance reasons only.)
	 *
	 * @see <a href=
	 *      "https://www.sqlite.org/c3ref/c_deterministic.html">SQLite</a>
	 */
	int SQLITE_DIRECTONLY = 0x000080000;

	/**
	 * Flag direct from SQLite. Used to mark a function as "safe".
	 *
	 * <blockquote>The {@code SQLITE_INNOCUOUS} flag means that the function is
	 * unlikely to cause problems even if misused. An innocuous function should
	 * have no side effects and should not depend on any values other than its
	 * input parameters. The {@code abs()} function is an example of an
	 * innocuous function. The {@code load_extension()} SQL function is not
	 * innocuous because of its side effects.
	 * <p>
	 * {@code SQLITE_INNOCUOUS} is similar to
	 * {@link org.sqlite.Function#FLAG_DETERMINISTIC SQLITE_DETERMINISTIC}, but
	 * is not exactly the same. The {@code random()} function is an example of a
	 * function that is innocuous but not deterministic.
	 * <p>
	 * Some heightened security settings ({@code SQLITE_DBCONFIG_TRUSTED_SCHEMA}
	 * and {@code PRAGMA trusted_schema=OFF}) disable the use of SQL functions
	 * inside views and triggers and in schema structures such as <em>CHECK</em>
	 * constraints, <em>DEFAULT</em> clauses, expression indexes, partial
	 * indexes, and generated columns unless the function is tagged with
	 * {@code SQLITE_INNOCUOUS}. Most built-in functions are innocuous.
	 * Developers are advised to avoid using the {@code SQLITE_INNOCUOUS} flag
	 * for application-defined functions unless the function has been carefully
	 * audited and found to be free of potentially security-adverse side-effects
	 * and information-leaks. </blockquote>
	 * <p>
	 * Note that this engine marks non-innocuous functions as
	 * {@link #SQLITE_DIRECTONLY}; this is slightly over-eager, but likely
	 * correct.
	 *
	 * @see <a href=
	 *      "https://www.sqlite.org/c3ref/c_deterministic.html">SQLite</a>
	 */
	int SQLITE_INNOCUOUS = 0x000200000;
}
