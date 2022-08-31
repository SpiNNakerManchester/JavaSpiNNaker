/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

/**
 * Application identifiers are used by SCAMP to tie various resources to their
 * owners.
 *
 * @author Donal Fellows
 */
public final class AppID {
	/**
	 * Maximum app ID.
	 */
	private static final int MAX_APP_ID = 255;

	/**
	 * The default application ID, used when code doesn't have a better idea.
	 */
	public static final AppID DEFAULT = new AppID(0);

	/**
	 * The application ID.
	 */
	public final int appID;

	/**
	 * Create an application ID.
	 *
	 * @param appID
	 *            The proposed ID of the application. Must be between 0 and 255.
	 * @throws IllegalArgumentException
	 *             If an illegal ID is given.
	 */
	public AppID(int appID) {
		if (appID < 0 || appID > MAX_APP_ID) {
			throw new IllegalArgumentException(
					"appID must be between 0 and " + MAX_APP_ID);
		}
		this.appID = appID;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof AppID) {
			var other = (AppID) o;
			return appID == other.appID;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (appID << 5) ^ 1236984681;
	}
}
