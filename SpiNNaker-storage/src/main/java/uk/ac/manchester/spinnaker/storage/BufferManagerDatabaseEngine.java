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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;

import java.io.File;
import java.io.IOException;

import uk.ac.manchester.spinnaker.storage.sqlite.SQLiteBufferStorage;

/**
 * A database interface for talking to the buffer manager database.
 *
 * @author Donal Fellows
 */
public class BufferManagerDatabaseEngine
		extends DatabaseEngine<BufferManagerStorage> {
	private static String sqlDDL;
	static {
		try {
			sqlDDL = resourceToString("/buffer_manager.sql", UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(
					"failed to read buffer manager database definition SQL", e);
		}
	}

	/**
	 * Create an engine interface for an in-memory database.
	 */
	public BufferManagerDatabaseEngine() {
	}

	/**
	 * Create an engine interface for a particular database.
	 *
	 * @param dbFile
	 *            The file containing the database.
	 */
	public BufferManagerDatabaseEngine(File dbFile) {
		super(dbFile);
	}

	@Override
	public String getDDL() {
		return sqlDDL;
	}

	@Override
	public BufferManagerStorage getStorageInterface() {
		return new SQLiteBufferStorage(this);
	}
}
