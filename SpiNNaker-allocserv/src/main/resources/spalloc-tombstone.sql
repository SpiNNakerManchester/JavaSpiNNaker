-- Copyright (c) 2021 The University of Manchester
--
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see <http://www.gnu.org/licenses/>.

CREATE TABLE IF NOT EXISTS tombstone.jobs(
	job_id INTEGER NOT NULL,
	machine_id INTEGER,
	machine_name TEXT,
	owner INTEGER,
	owner_name TEXT,
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
