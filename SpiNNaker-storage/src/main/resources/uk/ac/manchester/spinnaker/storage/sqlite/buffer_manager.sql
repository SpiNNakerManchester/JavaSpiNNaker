-- Copyright (c) 2018 The University of Manchester
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

-- https://www.sqlite.org/pragma.html#pragma_synchronous
PRAGMA main.synchronous = OFF;

-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing the cores.
CREATE TABLE IF NOT EXISTS core(
    core_id INTEGER PRIMARY KEY AUTOINCREMENT,
	x INTEGER NOT NULL,
	y INTEGER NOT NULL,
	processor INTEGER NOT NULL,
	core_name STRING);
-- Every processor has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS coreSanity ON core(
	x ASC, y ASC, processor ASC);

CREATE TABLE IF NOT EXISTS setup(
    setup_id INTEGER PRIMARY KEY CHECK (setup_id = 0),
    hardware_time_step_ms FLOAT NOT NULL,
    time_scale_factor INTEGER);

-- A table containing the metadata for an extraction run
CREATE TABLE IF NOT EXISTS extraction(
	extraction_id INTEGER PRIMARY KEY ASC AUTOINCREMENT,
    run_timestep INTEGER NOT NULL,
    n_run INTEGER NOT NULL,
    n_loop INTEGER,
    extract_time INTEGER
    );
CREATE VIEW IF NOT EXISTS extraction_view AS
	SELECT extraction_id, run_timestep, run_timestep * hardware_time_step_ms as run_time_ms,
	       n_run, n_loop, datetime(extract_time/1000, 'unixepoch') AS extraction_time
    from extraction join setup;

-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table describing recording regions.
CREATE TABLE IF NOT EXISTS region(
	region_id INTEGER PRIMARY KEY AUTOINCREMENT,
	core_id INTEGER NOT NULL
		REFERENCES core(core_id) ON DELETE RESTRICT,
	local_region_index INTEGER NOT NULL,
	address INTEGER,
	content BLOB NOT NULL DEFAULT '',
	content_len INTEGER DEFAULT 0,
	fetches INTEGER NOT NULL DEFAULT 0,
	append_time INTEGER);
-- Every recording region has a unique vertex and index
CREATE UNIQUE INDEX IF NOT EXISTS regionSanity ON region(
	core_id ASC, local_region_index ASC);

CREATE VIEW IF NOT EXISTS region_view AS
	SELECT core_id, region_id, x, y, processor, local_region_index
FROM core NATURAL JOIN region;

CREATE TABLE IF NOT EXISTS region_data(
    region_data_id INTEGER PRIMARY KEY AUTOINCREMENT,
	region_id INTEGER NOT NULL
		REFERENCES region(region_id) ON DELETE RESTRICT,
    extraction_id INTEGER NOT NULL
		REFERENCES extraction(extraction_id) ON DELETE RESTRICT,
	content BLOB NOT NULL,
	content_len INTEGER NOT NULL,
    missing_data INTEGER NOT NULL);
-- Every recording region is extracted once per BefferExtractor run
CREATE UNIQUE INDEX IF NOT EXISTS region_data_sanity ON region_data(
	region_id ASC, extraction_id ASC);

CREATE VIEW IF NOT EXISTS region_data_view AS
	SELECT core_id, region_id, extraction_id, x, y, processor, local_region_index,
		content, content_len
FROM region_view NATURAL JOIN region_data;

CREATE VIEW IF NOT EXISTS region_data_plus_view AS
	SELECT core_id, region_id, extraction_id, x, y, processor, local_region_index,
		content, content_len, run_timestep, run_time_ms, n_run, n_loop, extraction_time
FROM region_data_view NATURAL JOIN extraction_view;
