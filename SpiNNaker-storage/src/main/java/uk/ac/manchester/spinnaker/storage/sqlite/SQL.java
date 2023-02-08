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
	static final String INSERT_REGION = "INSERT INTO "
			+ "region(core_id, local_region_index, address)"
			+ " VALUES (?, ?, ?)";

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
	@GeneratesID
	static final String ADD_EXTRA_CONTENT =
			"INSERT INTO region_extra(region_id, content, content_len) "
					+ "VALUES (?, CAST(? AS BLOB), ?)";

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
	@ResultColumn("memory_used")
	static final String LIST_CORES_TO_LOAD =
			"SELECT core_id, x, y, processor, app_id, memory_used "
					+ "FROM core_view "
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
	@ResultColumn("memory_used")
	static final String LIST_CORES_TO_LOAD_FILTERED =
			"SELECT core_id, x, y, processor, app_id, memory_used "
					+ "FROM core_view "
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

	/**
	 * The name of the result containing the spalloc URI.
	 */
	static final String SPALLOC_URI = "service uri";

	/**
	 * The name of the result containing the proxy URI.
	 */
	static final String PROXY_URI = "job uri";

	/**
	 * The name of the result containing the proxy Authorization.
	 */
	static final String PROXY_AUTH = "Authorization";

	/**
	 * Get information about the proxy.
	 */
	static final String GET_PROXY_INFORMATION =
			"SELECT name, value FROM proxy_configuration WHERE "
			+ "((kind = 'SPALLOC' AND name = '" + PROXY_URI + "') OR"
			+ " (kind = 'HEADER' AND name = '" + PROXY_AUTH + "') OR"
			+ " (kind = 'SPALLOC' AND name = '" + SPALLOC_URI + "'))";
}
