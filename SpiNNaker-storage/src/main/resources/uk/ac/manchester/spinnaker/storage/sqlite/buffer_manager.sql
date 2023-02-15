-- Copyright (c) 2018 The University of Manchester
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
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
	processor INTEGER NOT NULL);
-- Every processor has a unique ID
CREATE UNIQUE INDEX IF NOT EXISTS coreSanity ON core(
	x ASC, y ASC, processor ASC);


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

-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
-- A table containing the data which doesn't fit in the content column of the
-- region table; care must be taken with this to not exceed 1GB! We actually
-- store one per auto-pause-resume cycle as that is more efficient.
CREATE TABLE IF NOT EXISTS region_extra(
	extra_id INTEGER PRIMARY KEY ASC AUTOINCREMENT,
	region_id INTEGER NOT NULL
		REFERENCES region(region_id) ON DELETE RESTRICT,
	content BLOB NOT NULL DEFAULT '',
	content_len INTEGER DEFAULT 0);

CREATE VIEW IF NOT EXISTS region_view AS
	SELECT core_id, region_id, x, y, processor, local_region_index, address,
		content, content_len, fetches, append_time,
		(fetches > 1) AS have_extra
FROM core NATURAL JOIN region;

CREATE VIEW IF NOT EXISTS extra_view AS
    SELECT core_id, region_id, extra_id, x, y, processor, local_region_index,
    	address, append_time, region_extra.content AS content,
    	region_extra.content_len AS content_len
FROM core NATURAL JOIN region NATURAL JOIN region_extra;
