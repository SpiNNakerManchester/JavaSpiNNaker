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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.security.SecureRandom;

import javax.annotation.PostConstruct;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Misc services related to password handling.
 *
 * @author Donal Fellows
 */
@Component
public class PasswordServices {
	private static final int BCRYPT_STRENGTH = 10;

	private static final int PASSWORD_LENGTH = 16;

	private PasswordEncoder passwordEncoder;

	private SecureRandom rng;

	@PostConstruct
	private void init() {
		rng = new SecureRandom();
		passwordEncoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH, rng);
	}

	/**
	 * Generate a random password.
	 *
	 * @return A password consisting of 16 random ASCII printable
	 *         characters.
	 */
	public final String generatePassword() {
		StringBuilder sb = new StringBuilder();
		rng.ints(PASSWORD_LENGTH, '\u0021', '\u007f')
				.forEachOrdered(c -> sb.append((char) c));
		return sb.toString();
	}

	/**
	 * Encode a password with bcrypt. <em>This is a slow operation! Do not
	 * hold a database transaction open when calling this.</em>
	 *
	 * @param password
	 *            The password to encode. May be {@code null}.
	 * @return The encoded password. Will be {@code null} if the input is
	 *         {@code null}.
	 */
	public final String encodePassword(String password) {
		return isNull(password) ? null : passwordEncoder.encode(password);
	}

	/**
	 * Check using bcrypt if a password matches its encoded form retrieved
	 * from the database. <em>This is a slow operation! Do not hold a
	 * database transaction open when calling this.</em>
	 *
	 * @param password
	 *            The password to check. If {@code null}, the encoded form
	 *            must also be {@code null} for a match to be true.
	 * @param encodedPassword
	 *            The encoded form from the database.
	 * @return True if the password matches, false otherwise.
	 */
	public final boolean matchPassword(String password,
			String encodedPassword) {
		if (isNull(password)) {
			return isNull(encodedPassword);
		}
		return nonNull(encodedPassword)
				&& passwordEncoder.matches(password, encodedPassword);
	}
}
