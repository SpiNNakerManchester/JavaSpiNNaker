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
	height INTEGER NOT NULL,
	board_model INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS tags (
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE CASCADE,
	tag TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS boards (
	board_id INTEGER PRIMARY KEY AUTOINCREMENT,
	address TEXT UNIQUE NOT NULL, -- IP address
	bmp_id INTEGER NOT NULL REFERENCES bmp(bmp_id) ON DELETE RESTRICT,
	board_num INTEGER NOT NULL, -- for use with the BMP
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE RESTRICT,
	x INTEGER NOT NULL, -- Board coordinate
	y INTEGER NOT NULL, -- Board coordinate
	root_x INTEGER NOT NULL, -- Chip coordinate
	root_y INTEGER NOT NULL, -- Chip coordinate
	allocated_job INTEGER REFERENCES jobs(job_id),
	board_power INTEGER,
	power_off_timestamp INTEGER, -- timestamp
	power_on_timestamp INTEGER, -- timestamp
	functioning INTEGER, -- boolean
	may_be_allocated INTEGER GENERATED ALWAYS AS ( -- generated column
		allocated_job IS NULL AND (functioning IS NULL OR functioning != 0)
		) VIRTUAL
);
-- Every board has a unique location within its machine
CREATE UNIQUE INDEX IF NOT EXISTS boardSanity ON boards(
	root_x ASC, root_y ASC, machine_id ASC);

CREATE TABLE IF NOT EXISTS bmp (
	bmp_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE RESTRICT,
	address TEXT UNIQUE NOT NULL, -- IP address
	cabinet INTEGER NOT NULL,
	frame INTEGER NOT NULL
);
-- Every BMP has a unique location within its machine
CREATE UNIQUE INDEX IF NOT EXISTS bmpSanity ON bmp(
    machine_id, cabinet, frame
);

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
	cabinet INTEGER,
	frame INTEGER,
	board INTEGER,
	max_dead_boards INTEGER NOT NULL DEFAULT (0)
);

CREATE TABLE IF NOT EXISTS pending_changes (
    change_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER REFERENCES jobs(job_id) ON DELETE RESTRICT,
    board_id INTEGER UNIQUE NOT NULL REFERENCES boards(board_id) ON DELETE RESTRICT,
    "power" INTEGER NOT NULL, -- Whether to switch the board on
    fpga_n INTEGER NOT NULL, -- Whether to switch the northward FPGA on
    fpga_s INTEGER NOT NULL, -- Whether to switch the southward FPGA on
    fpga_e INTEGER NOT NULL, -- Whether to switch the eastward FPGA on
    fpga_w INTEGER NOT NULL, -- Whether to switch the westward FPGA on
    fpga_nw INTEGER NOT NULL, -- Whether to switch the nothwest FPGA on
    fpga_se INTEGER NOT NULL -- Whether to switch the southeast FPGA on
);

CREATE TABLE IF NOT EXISTS board_model_coords(
	-- We never need the identitites of the rows
	model INTEGER NOT NULL,
	chip_x INTEGER NOT NULL,
	chip_y INTEGER NOT NULL
);
-- Board chip descriptors are unique
CREATE UNIQUE INDEX IF NOT EXISTS chipUniqueness ON board_model_coords(
    model, chip_x, chip_y
);

-- The information about chip configuration of boards
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
VALUES
	-- Version 3 boards
	(3, 0, 0), (3, 0, 1),
	(3, 1, 0), (3, 1, 1),
	-- Version 5 boards
	(5, 0, 0), (5, 0, 1), (5, 0, 2), (5, 0, 3),
	(5, 1, 0), (5, 1, 1), (5, 1, 2), (5, 1, 3), (5, 1, 4),
	(5, 2, 0), (5, 2, 1), (5, 2, 2), (5, 2, 3), (5, 2, 4), (5, 2, 5),
	(5, 3, 0), (5, 3, 1), (5, 3, 2), (5, 3, 3), (5, 3, 4), (5, 3, 5), (5, 3, 6),
	(5, 4, 0), (5, 4, 1), (5, 4, 2), (5, 4, 3), (5, 4, 4), (5, 4, 5), (5, 4, 6), (5, 4, 7),
			   (5, 5, 1), (5, 5, 2), (5, 5, 3), (5, 5, 4), (5, 5, 5), (5, 5, 6), (5, 5, 7),
						  (5, 6, 2), (5, 6, 3), (5, 6, 4), (5, 6, 5), (5, 6, 6), (5, 6, 7),
									 (5, 7, 3), (5, 7, 4), (5, 7, 5), (5, 7, 6), (5, 7, 7);

-- Create boards rarely seen in the wild
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 2, chip_x, chip_y FROM board_model_coords WHERE model = 3;
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 4, chip_x, chip_y FROM board_model_coords WHERE model = 5;
