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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indicates a class that can provide database connections to
 * suitably-configured databases.
 *
 * @author Donal Fellows
 * @param <APIType>
 *            The type of the higher-level access interface that can be used to
 *            work with the database this class makes connections to.
 */
public interface ConnectionProvider<APIType extends DatabaseAPI> {
	/**
	 * Get a connection to a database, creating it if needed.
	 *
	 * @return The configured connection to the database. The database will have
	 *         been seeded with DDL if necessary.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	Connection getConnection() throws SQLException;

	/**
	 * @return a storage interface that is suitable for providing support for a
	 *         particular API.
	 */
	APIType getStorageInterface();
}
