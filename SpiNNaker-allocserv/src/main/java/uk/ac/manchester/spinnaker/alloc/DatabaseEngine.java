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
package uk.ac.manchester.spinnaker.alloc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.NORMAL;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;

import java.io.File;
import java.io.IOException;
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
public class DatabaseEngine {
	private static final Logger log = getLogger(DatabaseEngine.class);

	private static String sqlDDL;

	static {
		try {
			sqlDDL = resourceToString("/spalloc.sql", UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("failed to read database definition SQL",
					e);
		}
	}

	/** Busy timeout for SQLite, in milliseconds. */
	private static final int BUSY_TIMEOUT = 500;

	private boolean initialised;

	private String dbConnectionUrl;

	private SQLiteConfig config = new SQLiteConfig();

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public DatabaseEngine(File dbFile) {
		this.dbConnectionUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		log.info("will manage database at " + dbFile.getAbsolutePath());
		config.enforceForeignKeys(true);
		config.setSynchronous(NORMAL);
		config.setBusyTimeout(BUSY_TIMEOUT);
		config.setTransactionMode(IMMEDIATE);
	}

	public Connection getConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("opening database connection {}", dbConnectionUrl);
		}

		Connection conn = DriverManager.getConnection(dbConnectionUrl,
				config.toProperties());
		synchronized (this) {
			if (!initialised) {
				try (Statement statement = conn.createStatement()) {
					statement.executeUpdate(sqlDDL);
				}
				initialised = true;
			}
		}
		return conn;
	}
}
