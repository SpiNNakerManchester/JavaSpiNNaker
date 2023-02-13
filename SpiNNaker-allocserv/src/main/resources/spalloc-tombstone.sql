-- Copyright (c) 2021-2023 The University of Manchester
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

CREATE TABLE IF NOT EXISTS tombstone.jobs(
	job_id INTEGER NOT NULL,
	machine_id INTEGER,
	machine_name TEXT,
	owner INTEGER,
	owner_name TEXT,
	"group" INTEGER,
	group_name TEXT,
	create_timestamp INTEGER, -- timestamp
	width INTEGER,
	height INTEGER,
	"depth" INTEGER,
	root_id INTEGER,
	keepalive_interval INTEGER, -- duration in seconds
	keepalive_host TEXT, -- IP address
	death_reason TEXT,
	death_timestamp INTEGER, -- timestamp
	original_request BLOB,
	allocation_timestamp INTEGER,
	allocation_size INTEGER,
	lifetime_duration INTEGER GENERATED ALWAYS AS ( -- generated column
		death_timestamp - allocation_timestamp) VIRTUAL,
	resources_used INTEGER GENERATED ALWAYS AS ( -- generated column
		lifetime_duration * allocation_size) VIRTUAL
);
CREATE UNIQUE INDEX IF NOT EXISTS tombstone.jobs_ids_unique ON jobs(
	job_id ASC);

CREATE TABLE IF NOT EXISTS tombstone.board_allocations(
	alloc_id INTEGER NOT NULL,
	job_id INTEGER NOT NULL,
	board_id INTEGER NOT NULL,
	allocation_timestamp INTEGER
);
