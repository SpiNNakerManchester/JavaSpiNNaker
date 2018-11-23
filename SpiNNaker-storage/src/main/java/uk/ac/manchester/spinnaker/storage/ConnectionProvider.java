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
package uk.ac.manchester.spinnaker.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Indicates a class that can provide database connections to
 * suitably-configured SQLite databases.
 *
 * @author Donal Fellows
 */
public interface ConnectionProvider {
	/**
	 * Get a connection to an SQLite database, creating it if needed.
	 *
	 * @return The configured connection to the database. The database will have
	 *         been seeded with DDL if necessary.
	 * @throws SQLException
	 *             If anything goes wrong.
	 */
	Connection getConnection() throws SQLException;
}
