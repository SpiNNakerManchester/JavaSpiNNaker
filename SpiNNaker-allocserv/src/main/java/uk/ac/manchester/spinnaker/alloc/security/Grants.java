/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.security;

/**
 * The strings that describe the roles that may be granted.
 *
 * @author Donal Fellows
 */
public interface Grants {
	/**
	 * The authority used to grant a user permission to create jobs, manipulate
	 * them, and read their details. Note that many features are locked to the
	 * owner of the job or admins. Users should also have {@link #GRANT_READER}.
	 */
	String GRANT_USER = "ROLE_USER";

	/**
	 * The authority used to grant a user permission to get general machine
	 * information and summaries of jobs. Without this, only the service root
	 * (and the parts required for logging in) will be visible.
	 */
	String GRANT_READER = "ROLE_READER";

	/**
	 * The authority used to grant a user permission to use administrative
	 * actions. Admins should also have {@link #GRANT_USER} and
	 * {@link #GRANT_READER}.
	 */
	String GRANT_ADMIN = "ROLE_ADMIN";
}
