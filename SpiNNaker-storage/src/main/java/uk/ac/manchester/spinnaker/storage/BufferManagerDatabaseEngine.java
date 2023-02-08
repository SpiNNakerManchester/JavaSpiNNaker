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

import static uk.ac.manchester.spinnaker.storage.sqlite.ResourceLoader.loadResource;

import java.io.File;

import uk.ac.manchester.spinnaker.storage.sqlite.SQLiteBufferStorage;

/**
 * A database interface for talking to the buffer manager database.
 *
 * @author Donal Fellows
 */
public class BufferManagerDatabaseEngine
		extends DatabaseEngine<BufferManagerStorage> {
	private static String sqlDDL = loadResource("buffer_manager.sql");

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
