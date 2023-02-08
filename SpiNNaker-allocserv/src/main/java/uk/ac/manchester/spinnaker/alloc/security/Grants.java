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
