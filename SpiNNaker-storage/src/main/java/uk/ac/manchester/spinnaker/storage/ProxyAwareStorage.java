/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

/**
 * Storage interface that knows how to get information about the proxy system
 * from the DB.
 *
 * @author Donal Fellows
 */
public non-sealed interface ProxyAwareStorage extends DatabaseAPI {
	/**
	 * Get the proxy information from the database.
	 *
	 * @return The proxy information, or {@code null} if none defined. When
	 *         there is no proxy, only direct connections to SpiNNaker are
	 *         possible.
	 * @throws StorageException
	 *             If anything goes wrong.
	 */
	ProxyInformation getProxyInformation() throws StorageException;
}
