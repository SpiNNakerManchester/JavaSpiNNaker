/*
 * Copyright (c) 2021-2023 The University of Manchester
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
