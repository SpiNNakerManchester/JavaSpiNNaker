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

CREATE TABLE IF NOT EXISTS machines (
	machine_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_name TEXT UNIQUE NOT NULL,
	width INTEGER NOT NULL,
	height INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS tags (
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
	tag TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS boards (
	board_id INTEGER NOT NULL AUTOINCREMENT,
	address TEXT UNIQUE NOT NULL,
	bmp_address TEXT UNIQUE NOT NULL,
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE RESTRICT,
	root_x INTEGER NOT NULL,
	root_y INTEGER NOT NULL,
	allocated_job INTEGER REFERENCES jobs(job_id),
	board_power INTEGER,
	power_off_timestamp INTEGER, -- timestamp
	power_on_timestamp INTEGER, -- timestamp
	functioning INTEGER, -- boolean
	cabinet INTEGER NOT NULL,
	frame INTEGER NOT NULL,
	board_num INTEGER NOT NULL
);
-- Every board has a unique location within its machine
CREATE UNIQUE INDEX IF NOT EXISTS boardSanity ON boards(
	root_x ASC, root_y ASC, machine_id ASC);

CREATE TABLE IF NOT EXISTS jobs (
	job_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER REFERENCES machines(machine_id) ON DELETE RESTRICT,
	width INTEGER, -- set after allocation
	height INTEGER, -- set after allocation
	root_id INTEGER REFERENCES boards(board_id) ON DELETE RESTRICT, -- set after allocation
	job_state INTEGER DEFAULT (0),
	keepalive_timestamp INTEGER, -- timestamp
	keepalive_host TEXT, -- IP address
	num_pending INTEGER NOT NULL DEFAULT (0)
);

CREATE TABLE IF NOT EXISTS job_request (
	req_id INTEGER PRIMARY KEY AUTOINCREMENT,
	job_id INTEGER NOT NULL REFERENCES jobs(job_id),
	num_boards INTEGER,
	width INTEGER,
	height INTEGER,
	x INTEGER,
	y INTEGER,
	z INTEGER
);

CREATE TABLE IF NOT EXISTS pending_changes (
    change_id INTEGER NOT NULL AUTOINCREMENT,
    board_id INTEGER NOT NULL REFERENCES boards(board_id) ON DELETE RESTRICT,
    job_id INTEGER REFERENCES jobs(job_id) ON DELETE RESTRICT,
    reconfiguration BLOB -- TODO
);
