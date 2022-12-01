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

import org.springframework.security.core.GrantedAuthority;

import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.alloc.security.LocalAuthProviderImpl.CollabratoryAuthority;
import uk.ac.manchester.spinnaker.alloc.security.LocalAuthProviderImpl.OrganisationAuthority;

/**
 * Contains a single basic role grant.
 */
@Immutable
sealed class SimpleGrantedAuthority implements GrantedAuthority
		permits CollabratoryAuthority, OrganisationAuthority {
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
