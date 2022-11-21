/*
 * Copyright (c) 2022 The University of Manchester
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

import java.io.Serializable;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;

/**
 * A saved authentication token. Categorically only ever valid in the session
 * for which it was created; if the session changes, it cannot be reused.
 *
 * @author Donal Fellows
 */
final class Token implements Serializable {
	private static final long serialVersionUID = -439034988839648948L;

	private final String id;

	private final Authentication auth;

	Token(HttpSession s, Authentication a) {
		this.auth = a;
		this.id = s.getId();
	}

	boolean isValid(HttpSession s) {
		return s.getId().equals(id);
	}

	Authentication getAuth() {
		return auth;
	}
}
