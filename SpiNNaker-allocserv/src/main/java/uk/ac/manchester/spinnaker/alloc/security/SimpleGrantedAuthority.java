/*
 * Copyright (c) 2018-2023 The University of Manchester
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

import org.springframework.security.core.GrantedAuthority;

import com.google.errorprone.annotations.Immutable;

/**
 * Contains a single basic role grant.
 */
@Immutable
class SimpleGrantedAuthority implements GrantedAuthority {
	private static final long serialVersionUID = 7765648523730760900L;

	private final String role;

	/**
	 * @param role
	 *            The role in this authority.
	 */
	SimpleGrantedAuthority(String role) {
		this.role = role;
	}

	@Override
	public String getAuthority() {
		return role;
	}

	@Override
	public String toString() {
		return role;
	}
}
