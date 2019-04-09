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

import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameters;
import uk.ac.manchester.spinnaker.storage.ResultColumns;

/**
 * The actual queries used by the data access layer.
 *
 * @author Donal Fellows
 */
abstract class SQL {
	private SQL() {
	}

	// -----------------------------------------------------------------
	// Recording regions -----------------------------------------------
	// -----------------------------------------------------------------

	/** Create an (x,y,p) record. */
	@Parameters({
		"x", "y", "processor"
	})
	@GeneratesID
	static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?)";

	/** Find an existing (x,y,p) record. */
	@Parameters({
		"x", "y", "processor"
	})
	@ResultColumns("core_id")
	static final String GET_LOCATION = "SELECT core_id FROM core"
			+ " WHERE x = ? AND y = ? AND processor = ? LIMIT 1";

	/** Create an empty region record. */
	@Parameters({
		"core_id", "local_region_index", "address"
	})
	@GeneratesID
	static final String INSERT_REGION =
			"INSERT INTO region(core_id, local_region_index, address) "
					+ "VALUES (?, ?, ?)";

	/** Find an existing region record. */
	@Parameters({
		"core_id", "local_region_index"
	})
	@ResultColumns("region_id")
	static final String GET_REGION = "SELECT region_id FROM region WHERE "
			+ "core_id = ? AND local_region_index = ? LIMIT 1";

	/** Append content to a region record. */
	@Parameters({
		"content_to_append", "append_time", "region_id"
	})
	static final String APPEND_CONTENT =
			"UPDATE region SET content = content || ?, fetches = fetches + 1,"
					+ " append_time = ? WHERE region_id = ?";

	/** Fetch the current variable state of a region record. */
	@Parameters({
		"x", "y", "processor", "local_region_index"
	})
	@ResultColumns({"content", "fetches", "append_time"})
	static final String FETCH_RECORDING =
			"SELECT content, fetches, append_time FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " AND local_region_index = ? LIMIT 1";

	/** List the cores with storage. */
	@Parameters({})
	@ResultColumns({"x", "y", "processor"})
	static final String GET_CORES_WITH_STORAGE =
			"SELECT DISTINCT x, y, processor FROM region_view"
					+ " ORDER BY x, y, processor";

	/** List the regions of a core with storage. */
	@Parameters({
		"x", "y", "processor"
	})
	@ResultColumns("local_region_index")
	static final String GET_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT local_region_index FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " ORDER BY local_region_index";

	// -----------------------------------------------------------------
	// Data loading ----------------------------------------------------
	// -----------------------------------------------------------------

	/** Count the data specifications in the DB that are still to be run. */
	@Parameters({})
	@ResultColumns({"count_content"})
	static final String COUNT_WORK =
			"SELECT count(content) AS count_content FROM core_view "
					+ "WHERE start_address IS NULL AND "
					+ "app_id IS NOT NULL AND content IS NOT NULL";

	/** List the ethernets described in the database. */
	@Parameters({})
	@ResultColumns({"ethernet_id", "ethernet_x", "ethernet_y", "ip_address"})
	static final String LIST_ETHERNETS =
			"SELECT DISTINCT ethernet_id, ethernet_x, ethernet_y, ip_address"
					+ " FROM core_view";

	/** List the cores of a ethernets with a data specification to run. */
	@Parameters("ethernet_id")
	@ResultColumns({"core_id", "x", "y", "processor", "app_id"})
	static final String LIST_CORES_TO_LOAD =
			"SELECT core_id, x, y, processor, app_id FROM core_view "
					+ "WHERE ethernet_id = ? AND start_address IS NULL AND "
					+ "app_id IS NOT NULL AND content IS NOT NULL";

	/** Get the data specification to run for a particular core. */
	@Parameters("core_id")
	@ResultColumns("content")
	static final String GET_CORE_DATA_SPEC =
			"SELECT content FROM core_view WHERE core_id = ? LIMIT 1";

	/**
	 * Store the metadata about the loaded data generated by data specification
	 * execution.
	 */
	@Parameters({
		"start_address", "memory_used", "memory_written", "core_id"
	})
	static final String ADD_LOADING_METADATA = "UPDATE core "
			+ "SET start_address = ?, memory_used = ?, memory_written = ? "
			+ "WHERE core_id = ?";
}
