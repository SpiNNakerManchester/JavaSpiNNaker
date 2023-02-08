/*
 * Copyright (c) 2018 The University of Manchester
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

	@Override
	public String toString() {
		return "App#" + appID;
	}
}
