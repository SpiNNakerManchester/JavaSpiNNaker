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

import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.OFF;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.sqlite.SQLiteConfig;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 */
public abstract class DatabaseEngine implements ConnectionProvider {
	private static final Logger log = getLogger(DatabaseEngine.class);
	/** Busy timeout for SQLite, in milliseconds. */
	private static final int BUSY_TIMEOUT = 500;

	private String dbConnectionUrl;

	/**
	 * Create an engine interface for an in-memory database.
	 */
	public DatabaseEngine() {
		this.dbConnectionUrl = "jdbc:sqlite::memory:";
		log.info("will manage database in memory");
	}

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(File dbFile) {
		this.dbConnectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.info("will manage database at " + dbFile.getAbsolutePath());
	}

	@Override
	public Connection getConnection() throws SQLException {
		log.debug("opening database connection {}", dbConnectionUrl);
		SQLiteConfig config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		config.setSynchronous(OFF);
		config.setBusyTimeout(BUSY_TIMEOUT);
		Connection conn = DriverManager.getConnection(dbConnectionUrl,
				config.toProperties());
		try (Statement statement = conn.createStatement()) {
			statement.executeUpdate(getDDL());
		}
		return conn;
	}

	/**
	 * @return The DDL for initialising this kind of database.
	 */
	public abstract String getDDL();
}
