/*
 * Copyright (c) 2025 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.db;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import com.google.errorprone.annotations.CompileTimeConstant;

/**
 * Some sort of SQL that can be executed as part of a query or an update.
 */
public class SQL {

	private final String sql;

	/**
	 * Create a new SQL string.
	 *
	 * @param sql The String containing SQL
	 */
	public SQL(@CompileTimeConstant String sql) {
		this.sql = sql;
	}

	/**
	 * Create a new SQL string from a resource.
	 *
	 * @param resource The resource containing
	 */
	public SQL(Resource resource) {
		this(readResource(resource));
	}

	/**
	 * Create a new SQL string with parameters that can be replaced.
	 *
	 * @param sql The String containing SQL with replaceable parts
	 * @param parameters The parameters that are to be replaced
	 * @param values The values that the parameters are to be replaced with
	 */
	public SQL(String sql, List<String> parameters, List<String> values) {

		// Translate the string
		var sqlString = sql;

		// Replace the parameters with the values
		for (int i = 0; i < parameters.size(); i++) {
			sqlString = sqlString.replace(parameters.get(i), values.get(i));
		}
		System.err.println(sqlString);
		this.sql = sqlString;
	}

	/**
	 * Create a new SQL string with parameters that can be replaced.
	 *
	 * @param resource The resource containing SQL with replaceable parts
	 * @param parameters The parameters that are to be replaced
	 * @param values The values that the parameters are to be replaced with
	 */
	public SQL(Resource resource, List<String> parameters,
			List<String> values) {
		this(readResource(resource), parameters, values);
	}

	private static String readResource(Resource resource) {
		try (var is = resource.getInputStream()) {
			return IOUtils.toString(is, UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("Failed to read SQL resource", e);
		}
	}

	/**
	 * Get the SQL to be executed.
	 *
	 * @return The SQL.
	 */
	public String getSQL() {
		return sql;
	}
}
