/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.storage;

import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteConfig.SynchronousMode.OFF;
import static org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.sqlite.SQLiteConfig;

import com.google.errorprone.annotations.MustBeClosed;

/**
 * The database engine interface. Based on SQLite.
 *
 * @author Donal Fellows
 * @param <APIType>
 *            The type of the higher-level access interface that can be used to
 *            work with the database this class makes connections to.
 */
public abstract class DatabaseEngine<APIType extends DatabaseAPI>
		implements ConnectionProvider<APIType> {
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
		log.info("will manage database at {}", dbFile.getAbsolutePath());
	}

	@Override
	@MustBeClosed
	public Connection getConnection() throws SQLException {
		if (log.isDebugEnabled()) {
			log.debug("opening database connection {}", dbConnectionUrl);
		}
		var config = new SQLiteConfig();
		config.enforceForeignKeys(true);
		config.setSynchronous(OFF);
		config.setBusyTimeout(BUSY_TIMEOUT);
		config.setTransactionMode(IMMEDIATE);
		var conn = DriverManager.getConnection(dbConnectionUrl,
				config.toProperties());
		try (var statement = conn.createStatement()) {
			statement.executeUpdate(getDDL());
		}
		return conn;
	}

	/**
	 * @return The DDL for initialising this kind of database.
	 */
	public abstract String getDDL();
}
