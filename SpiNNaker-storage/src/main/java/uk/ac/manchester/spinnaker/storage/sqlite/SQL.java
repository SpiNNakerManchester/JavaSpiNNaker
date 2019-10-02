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
package uk.ac.manchester.spinnaker.storage.sqlite;

import uk.ac.manchester.spinnaker.storage.GeneratesID;
import uk.ac.manchester.spinnaker.storage.Parameter;
import uk.ac.manchester.spinnaker.storage.Parameters;
import uk.ac.manchester.spinnaker.storage.ResultColumn;

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
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@GeneratesID
	static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?)";

	/** Find an existing (x,y,p) record. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@ResultColumn("core_id")
	static final String GET_LOCATION = "SELECT core_id FROM core"
			+ " WHERE x = ? AND y = ? AND processor = ? LIMIT 1";

	/** Create an empty region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@Parameter("address")
	@GeneratesID
	static final String INSERT_REGION =
			"INSERT INTO region(core_id, local_region_index, address) "
					+ "VALUES (?, ?, ?)";

	/** Find an existing region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("region_id")
	static final String GET_REGION = "SELECT region_id FROM region WHERE "
			+ "core_id = ? AND local_region_index = ? LIMIT 1";

	/** Append content to a region record. */
	@Parameter("content_to_append")
	@Parameter("append_time")
	@Parameter("region_id")
	static final String APPEND_CONTENT =
			"UPDATE region SET content = content || ?, fetches = fetches + 1,"
					+ " append_time = ? WHERE region_id = ?";

	/** Prepare a region record for handling content in the extra table. */
	@Parameter("append_time")
	@Parameter("region_id")
	static final String PREP_EXTRA_CONTENT =
			"UPDATE region SET fetches = fetches + 1, append_time = ?, "
			+ "have_extra = 1 WHERE region_id = ?";

	/** Append content to the given row in the extra table. */
	@Parameter("content_to_append")
	@Parameter("extra_id")
	static final String APPEND_EXTRA_CONTENT =
			"UPDATE region_extra SET content = content || ? "
			+ "WHERE extra_id = ?";

	/** Add content to a new row in the extra table. */
	@Parameter("region_id")
	@Parameter("content_to_append")
	@GeneratesID
	static final String ADD_EXTRA_CONTENT =
			"INSERT INTO region_extra(region_id, content) VALUES (?, ?)";

	/**
	 * Discover whether region in the main region table is running out (or has
	 * already run out) of room.
	 */
	@Parameter("region_id")
	@ResultColumn("len")
	@ResultColumn("have_extra")
	static final String GET_MAIN_CONTENT_SIZE =
			"SELECT length(content) AS len, have_extra FROM region "
					+ "WHERE region_id = ? LIMIT 1";

	/**
	 * Determine what row of the extra table should receive the next chunk for a
	 * region, and report how much space is already used (which can in turn
	 * determine whether there's space left to add the chunk).
	 */
	@Parameter("region_id")
	@ResultColumn("len")
	@ResultColumn("extra_id")
	static final String GET_EXTRA_CONTENT_ROW =
			"SELECT length(content) AS len, extra_id FROM region_extra "
					+ "WHERE region_id = ? ORDER BY extra_id DESC LIMIT 1";

	/** Fetch the current variable state of a region record. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@Parameter("local_region_index")
	@ResultColumn("content")
	@ResultColumn("fetches")
	@ResultColumn("append_time")
	static final String FETCH_RECORDING =
			"SELECT content, fetches, append_time FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " AND local_region_index = ? LIMIT 1";

	/** List the cores with storage. */
	@Parameters({})
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("processor")
	static final String GET_CORES_WITH_STORAGE =
			"SELECT DISTINCT x, y, processor FROM region_view"
					+ " ORDER BY x, y, processor";

	/** List the regions of a core with storage. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@ResultColumn("local_region_index")
	static final String GET_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT local_region_index FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " ORDER BY local_region_index";

	// -----------------------------------------------------------------
	// Data loading ----------------------------------------------------
	// -----------------------------------------------------------------

	/** Count the data specifications in the DB that are still to be run. */
	@Parameters({})
	@ResultColumn("count_content")
	static final String COUNT_WORK =
			"SELECT count(content) AS count_content FROM core_view "
					+ "WHERE start_address IS NULL AND "
					+ "app_id IS NOT NULL AND content IS NOT NULL";

	/** List the ethernets described in the database. */
	@Parameters({})
	@ResultColumn("ethernet_id")
	@ResultColumn("ethernet_x")
	@ResultColumn("ethernet_y")
	@ResultColumn("ip_address")
	static final String LIST_ETHERNETS =
			"SELECT DISTINCT ethernet_id, ethernet_x, ethernet_y, ip_address"
					+ " FROM core_view";

	/** List the cores of a ethernets with a data specification to run. */
	@Parameter("ethernet_id")
	@ResultColumn("core_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("processor")
	@ResultColumn("app_id")
	static final String LIST_CORES_TO_LOAD =
			"SELECT core_id, x, y, processor, app_id FROM core_view "
					+ "WHERE ethernet_id = ? AND app_id IS NOT NULL "
					+ "AND content IS NOT NULL";

	/** List the cores of a ethernets with a data specification to run. */
	@Parameter("ethernet_id")
	@Parameter("is_system")
	@ResultColumn("core_id")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("processor")
	@ResultColumn("app_id")
	static final String LIST_CORES_TO_LOAD_FILTERED =
			"SELECT core_id, x, y, processor, app_id FROM core_view "
					+ "WHERE ethernet_id = ? AND is_system = ? "
					+ "AND app_id IS NOT NULL AND content IS NOT NULL";

	/** Get the data specification to run for a particular core. */
	@Parameter("core_id")
	@ResultColumn("content")
	static final String GET_CORE_DATA_SPEC =
			"SELECT content FROM core_view WHERE core_id = ? LIMIT 1";

	/**
	 * Store the metadata about the loaded data generated by data specification
	 * execution.
	 */
	@Parameter("start_address")
	@Parameter("memory_used")
	@Parameter("memory_written")
	@Parameter("core_id")
	static final String ADD_LOADING_METADATA = "UPDATE core "
			+ "SET start_address = ?, memory_used = ?, memory_written = ? "
			+ "WHERE core_id = ?";
}
