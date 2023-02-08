/*
 * Copyright (c) 2022 The University of Manchester
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

import java.io.Serializable;

import javax.servlet.http.HttpSession;

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
