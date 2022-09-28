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

import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_ADMIN;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_READER;
import static uk.ac.manchester.spinnaker.alloc.security.Grants.GRANT_USER;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.security.core.GrantedAuthority;

/**
 * Just how trusted is a user?
 */
public enum TrustLevel {
	/** Grants no real permissions at all. */
	BASIC(),
	/**
	 * Grants read-only permissions in addition to {@link #BASIC}.
	 *
	 * @see Grants#GRANT_READER
	 */
	READER(GRANT_READER),
	/**
	 * Grants job creation and management permissions in addition to
	 * {@link #READER}.
	 *
	 * @see Grants#GRANT_USER
	 */
	USER(GRANT_READER, GRANT_USER),
	/**
	 * Grants service administration permissions in addition to {@link #USER}.
	 *
	 * @see Grants#GRANT_ADMIN
	 */
	ADMIN(GRANT_READER, GRANT_USER, GRANT_ADMIN);

	@SuppressWarnings("ImmutableEnumChecker")
	private final List<String> grants;

	TrustLevel(String... grants) {
		this.grants = List.of(grants);
	}

	/**
	 * @return The authorities granted by this trust level.
	 */
	Stream<GrantedAuthority> getGrants() {
		return grants.stream().map(SimpleGrantedAuthority::new);
	}
}
