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
package uk.ac.manchester.spinnaker.storage.sqlite;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The actual queries used by the data access layer.
 *
 * @author Donal Fellows
 */
abstract class SQL {
	private SQL() {
	}

	// Recording regions
	@Parameters({
		"x", "y", "processor"
	})
	static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?)";

	@Parameters({
		"x", "y", "processor"
	})
	@ResultColumns("core_id")
	static final String GET_LOCATION = "SELECT core_id FROM core"
			+ " WHERE x = ? AND y = ? AND processor = ? LIMIT 1";

	@Parameters({
		"core_id", "local_region_index"
	})
	@ResultColumns("region_id")
	static final String GET_REGION = "SELECT region_id FROM region WHERE "
			+ "core_id = ? AND local_region_index = ? LIMIT 1";

	@Parameters({
		"core_id", "local_region_index", "address"
	})
	static final String INSERT_REGION =
			"INSERT INTO region(core_id, local_region_index, address) "
					+ "VALUES (?, ?, ?)";

	@Parameters({
		"content_to_append", "append_time", "region_id"
	})
	static final String APPEND_CONTENT =
			"UPDATE region SET content = content || ?, fetches = fetches + 1,"
					+ " append_time = ? WHERE region_id = ?";

	@Parameters({
		"x", "y", "processor", "local_region_index"
	})
	@ResultColumns({"content", "fetches", "append_time"})
	static final String FETCH_RECORDING =
			"SELECT content, fetches, append_time FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " AND local_region_index = ? LIMIT 1";

	@Parameters({})
	@ResultColumns({"x", "y", "processor"})
	static final String GET_CORES_WITH_STORAGE =
			"SELECT DISTINCT x, y, processor FROM region_view"
					+ " ORDER BY x, y, processor";

	@Parameters({
		"x", "y", "processor"
	})
	@ResultColumns("local_region_index")
	static final String GET_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT local_region_index FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " ORDER BY local_region_index";

	// Data loading
	@Parameters({})
	@ResultColumns({"board_id", "ethernet_x", "ethernet_y", "ethernet_address"})
	static final String LIST_BOARDS =
			"SELECT DISTINCT board_id, ethernet_x, ethernet_y, ethernet_address"
					+ " FROM core_view";

	@Parameters("board_id")
	@ResultColumns({"core_id", "x", "y", "processor", "content"})
	static final String LIST_CORES_TO_LOAD =
			"SELECT core_id, x, y, processor, content FROM core_view "
					+ "WHERE board_id = ? AND start_address IS NULL";

	@Parameters({
		"start_address", "memory_used", "memory_written", "core_id"
	})
	static final String ADD_LOADING_METADATA = "UPDATE core "
			+ "SET start_address = ?, memory_used = ?, memory_written = ? "
			+ "WHERE core_id = ?";

	/**
	 * Used to document what parameters are present in the DQL/DML.
	 *
	 * @author Donal Fellows
	 */
	@Retention(SOURCE)
	@Target(FIELD)
	@Documented
	@interface Parameters {
		/**
		 * Describes what parameters are supported by the statement once it is
		 * prepared.
		 *
		 * @return List of parameter names.
		 */
		String[] value();
	}

	/**
	 * Used to document what columns are supposed to be returned by the DQL.
	 *
	 * @author Donal Fellows
	 */
	@Retention(SOURCE)
	@Target(FIELD)
	@Documented
	@interface ResultColumns {
		/**
		 * Describes what columns are expected in the result.
		 *
		 * @return List of column names.
		 */
		String[] value();
	}
}
