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

	/** Create a recording region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("recording_region_id")
	static final String INSERT_RECORDING_REGION = "INSERT INTO"
			+ " recording_region(core_id, local_region_index)"
			+ " VALUES (?, ?) RETURNING recording_region_id";

	/** Create a download region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("download_region_id")
	static final String INSERT_DOWNLOAD_REGION = "INSERT INTO"
			+ " download_region(core_id, local_region_index)"
			+ " VALUES (?, ?) RETURNING download_region_id";

	/** For testing create an extraction record.

	Would normally be done by python.
	*/
	@ResultColumn("extraction_id")
	static final String INSERT_MOCK_EXTRACTION = "INSERT INTO "
			+ "extraction(run_timestep, n_run, n_loop, extract_time) "
			+ "VALUES(12345, 1, NULL, 987654) RETURNING extraction_id ";

	/** Find an existing recording region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("recoding_region_id")
	static final String GET_RECORDING_REGION = "SELECT recording_region_id"
			+ " FROM recording_region "
			+ " WHERE core_id = ? AND local_region_index = ? LIMIT 1";

	/** Find an existing download region record. */
	@Parameter("core_id")
	@Parameter("local_region_index")
	@ResultColumn("download_region_id")
	static final String GET_DOWNLOAD_REGION = "SELECT download_region_id "
			+ " FROM download_region"
			+ " WHERE core_id = ? AND local_region_index = ? LIMIT 1";

	/** Find the current extraction_id. */
	@ResultColumn("max_id")
	static final String GET_LAST_EXTRACTION_ID =
			"SELECT max(extraction_id) as max_id "
			+ "FROM extraction LIMIT 1";

	/** Create a recoding data record. */
	@Parameter("recording_region_id")
	@Parameter("extraction_id")
	@Parameter("content_to_add")
	@Parameter("content_len")
	@ResultColumn("recording_data_id")
	static final String ADD_RECORDING_DATA =
		"INSERT INTO recording_data(recording_region_id, extraction_id, "
			+ " content, content_len, missing_data) "
			+ "VALUES (?, ?, CAST(? AS BLOB), ?, 0) "
			+ "RETURNING recording_data_id";

	/** Create a recoding data record. */
	@Parameter("download_region_id")
	@Parameter("extraction_id")
	@Parameter("content_to_add")
	@Parameter("content_len")
	@ResultColumn("download_data_id")
	static final String ADD_DOWNLOAD_DATA =
		"INSERT INTO download_data(download_region_id, extraction_id, "
			+ " content, content_len, missing_data) "
			+ "VALUES (?, ?, CAST(? AS BLOB), ?, 0) "
			+ "RETURNING download_data_id";

	/** Fetch the current variable state of a region record. */
	@Parameter("recording_region_id")
	@ResultColumn("content")
	@ResultColumn("missing_data")
	static final String GET_RECORDING =
			"SELECT content, missing_data FROM recording_data "
				+ "WHERE recording_region_id = ? ORDER BY extraction_id ASC";

	/** Fetch the current variable state of a region record. */
	@Parameter("download_region_id")
	@ResultColumn("content")
	@ResultColumn("missing_data")
	static final String GET_DOWNLOAD =
		"SELECT content, missing_data FROM download_data "
			+ "WHERE download_region_id = ? ORDER BY extraction_id DESC "
			+ "LIMIT 1";

	/** List the cores with storage. */
	@Parameters({})
	@ResultColumn("x")
	@ResultColumn("y")
	@ResultColumn("processor")
	static final String GET_CORES_WITH_STORAGE =
			"SELECT DISTINCT x, y, processor FROM recording_data_view "
			+ "UNION "
			+ "SELECT DISTINCT x, y, processor FROM download_data_view "
			+ "ORDER BY x, y, processor;";

	/** List the regions of a core with storage. */
	@Parameter("x")
	@Parameter("y")
	@Parameter("processor")
	@ResultColumn("local_region_index")
	static final String GET_REGIONS_WITH_STORAGE =
			"SELECT DISTINCT local_region_index FROM "
			+ "("
			+ "SELECT local_region_index, x, y, processor "
			+ "FROM recording_region_view "
			+ "UNION "
			+ "SELECT local_region_index, x, y, processor "
			+ "FROM download_region_view "
			+ ") "
			+ "WHERE x = ? AND y = ? AND processor = ? "
			+ "ORDER BY local_region_index";

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
			"SELECT app_id FROM info";

	/** Get he machine dimensions. */
	@ResultColumn("width")
	@ResultColumn("height")
	static final String GET_MACHINE_DIMENSIONS =
			"SELECT width, height FROM info";

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
