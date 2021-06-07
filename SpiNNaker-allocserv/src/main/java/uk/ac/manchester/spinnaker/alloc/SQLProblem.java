/*
 * Copyright (c) 2018-2021 The University of Manchester
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

import java.sql.SQLException;

/**
 * A wrapper round {@link SQLException} that makes it into a runtime exception.
 *
 * @author Donal Fellows
 */
public final class SQLProblem extends RuntimeException {
	private static final long serialVersionUID = -2487090623988898303L;

	/**
	 * Create an instance.
	 *
	 * @param message
	 *            Describe what was being done that had a problem
	 * @param cause
	 *            The cause of the problem
	 */
	public SQLProblem(String message, SQLException cause) {
		super(message, cause);
	}

	/**
	 * Create an instance. Used to create a higher-level version of a wrapper,
	 * <em>discarding</em> the intermediate wrapper.
	 *
	 * @param message
	 *            Describe what was being done that had a problem
	 * @param cause
	 *            The wrapper around the cause of the problem
	 */
	public SQLProblem(String message, SQLProblem cause) {
		super(message, cause.getCause());
	}
}
