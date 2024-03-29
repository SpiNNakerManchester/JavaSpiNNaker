/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.storage.sqlite;

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
	@ResultColumn("core_id")
	static final String INSERT_LOCATION =
			"INSERT INTO core(x, y, processor) VALUES(?, ?, ?) "
					+ "RETURNING core_id";

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
	@ResultColumn("region_id")
	static final String INSERT_REGION = "INSERT INTO "
			+ "region(core_id, local_region_index, address)"
			+ " VALUES (?, ?, ?) RETURNING region_id";

	/** Find an existing region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("region_id")
	static final String GET_REGION = "SELECT region_id FROM region WHERE "
			+ "core_id = ? AND local_region_index = ? LIMIT 1";

	/** Append content to a region record. */
	@Parameter("content_to_add")
	@Parameter("content_len")
	@Parameter("append_time")
	@Parameter("region_id")
	static final String ADD_CONTENT =
			"UPDATE region SET content = CAST(? AS BLOB), content_len = ?, "
					+ "fetches = 1, append_time = ? WHERE region_id = ?";

	/** Prepare a region record for handling content in the extra table. */
	@Parameter("append_time")
	@Parameter("region_id")
	static final String PREP_EXTRA_CONTENT =
			"UPDATE region SET fetches = fetches + 1, append_time = ? "
			+ "WHERE region_id = ?";

	/** Add content to a new row in the extra table. */
	@Parameter("region_id")
	@Parameter("content_to_add")
	@Parameter("content_len")
	@ResultColumn("extra_id")
	static final String ADD_EXTRA_CONTENT =
			"INSERT INTO region_extra(region_id, content, content_len) "
					+ "VALUES (?, CAST(? AS BLOB), ?) RETURNING extra_id";

	/**
	 * Discover whether region in the main region table is available for storing
	 * data.
	 */
	@Parameter("region_id")
	@ResultColumn("existing")
	static final String GET_MAIN_CONTENT_AVAILABLE =
			"SELECT COUNT(*) AS existing FROM region "
					+ "WHERE region_id = ? AND fetches = 0";

	/**
	 * Determine just how much content there is for a row, overall.
	 */
	@Parameter("region_id")
	@ResultColumn("len")
	static final String GET_CONTENT_TOTAL_LENGTH =
			"SELECT r.content_len + ("
					+ "    SELECT SUM(x.content_len) "
					+ "    FROM region_extra AS x "
					+ "    WHERE x.region_id = r.region_id"
					+ ") AS len FROM region AS r WHERE region_id = ?";

	/** Fetch the current variable state of a region record. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@Parameter("local_region_index")
	@ResultColumn("content")
	@ResultColumn("content_len")
	@ResultColumn("fetches")
	@ResultColumn("append_time")
	@ResultColumn("region_id")
	static final String FETCH_RECORDING =
			"SELECT content, content_len, fetches, append_time, region_id "
					+ "FROM region_view"
					+ " WHERE x = ? AND y = ? AND processor = ?"
					+ " AND local_region_index = ? LIMIT 1";

	/** Fetch the current variable state of a region record. */
	@Parameter("region_id")
	@ResultColumn("content")
	@ResultColumn("content_len")
	static final String FETCH_EXTRA_RECORDING =
			"SELECT content, content_len FROM region_extra"
					+ " WHERE region_id = ? ORDER BY extra_id ASC";

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

	/**
	 * List the Ethernet-enabled chips described in the database. This is
	 * effectively the list of boards.
	 */
	@Parameters({})
	@ResultColumn("ethernet_x")
	@ResultColumn("ethernet_y")
	@ResultColumn("ip_address")
	static final String LIST_ETHERNETS =
			"SELECT DISTINCT ethernet_x, ethernet_y, ip_address"
					+ " FROM core_view";

	/**
	 * List the cores of a board (by its Ethernet-enabled chip location) with
	 * data to load. This picks either system or application cores.
	 */
	@Parameter("ethernet_x")
	@Parameter("ethernet_y")
	@Parameter("is_system")
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("p")
	static final String LIST_CORES_TO_LOAD =
			"SELECT x, y, p "
					+ "FROM core_view "
					+ "WHERE ethernet_x = ? AND ethernet_y = ? "
					+ "AND is_system = ? ";

	/** List the regions and sizes of a chip with data to load. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("p")
	@ResultColumn("region_num")
	@ResultColumn("size")
	static final String GET_REGION_SIZES =
			"SELECT region_num, size "
					+ "FROM region "
					+ "WHERE x = ? AND y = ? AND p = ? "
					+ "ORDER BY region_num";

	/** Get the data to load for a particular core. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("p")
	@ResultColumn("region_num")
	@ResultColumn("content")
	@ResultColumn("pointer")
	static final String GET_REGION_POINTER_AND_CONTEXT =
			"SELECT region_num, content, pointer "
					+ "FROM pointer_content_view "
					+ "WHERE x = ? AND y = ? AND p = ? "
					+ "ORDER BY region_num";

	/**
	 * Store the start_address for the core.
	 */
	@Parameter("start_address")
	@Parameter("x")
	@Parameter("y")
	@Parameter("p")
	static final String SET_START_ADDRESS = "UPDATE core "
			+ "SET start_address = ? "
			+ "WHERE x = ? AND y = ? AND p = ?";

	/** Get the start address for this core. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("p")
	@ResultColumn("start_address")
	static final String GET_START_ADDRESS =
			"SELECT start_address "
					+ "FROM core "
					+ "WHERE x = ? AND y = ? AND p = ? ";

	/**
	 * Store the pointer for this reason.
	 */
	@Parameter("pointer")
	@Parameter("x")
	@Parameter("y")
	@Parameter("p")
	@Parameter("region_num")
	static final String SET_REGION_POINTER = "UPDATE region "
			+ "SET pointer = ?"
			+ "WHERE x = ? AND y = ? and p = ? and region_num = ?";

	/** Get the app_id. */
	@ResultColumn("app_id")
	static final String GET_APP_ID =
			"SELECT app_id "
					+ "FROM app_id ";

	/**
	 * The name of the result containing the spalloc URI.
	 */
	static final String SPALLOC_URI = "service uri";

	/**
	 * The name of the result containing the proxy URI.
	 */
	static final String PROXY_URI = "job uri";

	/**
	 * The kind of the result containing a proxy cookie.
	 */
	static final String COOKIE = "COOKIE";

	/**
	 * The kind of the result containing a proxy header.
	 */
	static final String HEADER = "HEADER";

	/**
	 * The kind of the result containing a spalloc information.
	 */
	static final String SPALLOC = "SPALLOC";

	/**
	 * Get information about the proxy.
	 */
	static final String GET_PROXY_INFORMATION =
			"SELECT kind, name, value FROM proxy_configuration WHERE "
			+ "((kind = '" + SPALLOC + "' AND name = '" + PROXY_URI + "') OR"
			+ " (kind = '" + HEADER +  "') OR (kind = '" + COOKIE + "') OR"
			+ " (kind = '" + SPALLOC + "' AND name = '" + SPALLOC_URI + "'))";
}
