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

CREATE TABLE IF NOT EXISTS directions(
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS movement_directions(
	z INTEGER NOT NULL,
	direction INTEGER NOT NULL REFERENCES directions("id") ON DELETE RESTRICT,
	dx INTEGER NOT NULL,
	dy INTEGER NOT NULL,
	dz INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS movementUniqueness ON movement_directions(
	z ASC, direction ASC
);

CREATE VIEW IF NOT EXISTS motions AS
SELECT z, directions.name AS dir, dx, dy, dz
FROM movement_directions JOIN directions
	ON movement_directions.direction = directions.id;

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
CREATE UNIQUE INDEX IF NOT EXISTS boardCoordinateSanity ON boards(
	root_x ASC, root_y ASC, machine_id ASC);
-- Every board has a unique board number within its BMP
CREATE UNIQUE INDEX IF NOT EXISTS boardBmpSanity ON boards(
	bmp_id ASC, board_num ASC);

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

CREATE TABLE IF NOT EXISTS links (
	link_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_1 INTEGER NOT NULL REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_1 INTEGER NOT NULL REFERENCES directions(id) ON DELETE RESTRICT,
	board_2 INTEGER NOT NULL REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_2 INTEGER NOT NULL REFERENCES directions(id) ON DELETE RESTRICT,
	live INTEGER NOT NULL DEFAULT (1)
);
CREATE UNIQUE INDEX IF NOT EXISTS link_1 ON links(
	board_1 ASC, dir_1 ASC);
CREATE UNIQUE INDEX IF NOT EXISTS link_2 ON links(
	board_2 ASC, dir_2 ASC);
CREATE UNIQUE INDEX IF NOT EXISTS only_one_link_between_boards ON links(
	board_1 ASC, board_2 ASC);

CREATE TABLE IF NOT EXISTS jobs (
	job_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER REFERENCES machines(machine_id) ON DELETE RESTRICT,
	owner TEXT NOT NULL,
	create_timestamp INTEGER NOT NULL,
	width INTEGER, -- set after allocation
	height INTEGER, -- set after allocation
	root_id INTEGER REFERENCES boards(board_id) ON DELETE RESTRICT, -- set after allocation
	job_state INTEGER DEFAULT (0),
	keepalive_interval INTEGER NOT NULL,
	keepalive_timestamp INTEGER, -- timestamp
	keepalive_host TEXT, -- IP address
	death_reason TEXT,
	death_timestamp INTEGER, -- timestamp
	num_pending INTEGER NOT NULL DEFAULT (0)
);

CREATE TABLE IF NOT EXISTS job_request (
	req_id INTEGER PRIMARY KEY AUTOINCREMENT,
	job_id INTEGER NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
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
    job_id INTEGER REFERENCES jobs(job_id) ON DELETE CASCADE,
    board_id INTEGER UNIQUE NOT NULL REFERENCES boards(board_id) ON DELETE RESTRICT,
    "power" INTEGER NOT NULL, -- Whether to switch the board on
    fpga_n INTEGER NOT NULL, -- Whether to switch the northward FPGA on
    fpga_s INTEGER NOT NULL, -- Whether to switch the southward FPGA on
    fpga_ne INTEGER NOT NULL, -- Whether to switch the northeast FPGA on
    fpga_nw INTEGER NOT NULL, -- Whether to switch the northwest FPGA on
    fpga_se INTEGER NOT NULL, -- Whether to switch the southeast FPGA on
    fpga_sw INTEGER NOT NULL -- Whether to switch the southwest FPGA on
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

BEGIN;

-- The information about chip configuration of boards
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
VALUES
	-- Version 3 boards
	(3, 0, 1), (3, 1, 1),
	(3, 0, 0), (3, 1, 0),
	-- Version 5 boards
	                                            (5, 4, 7), (5, 5, 7), (5, 6, 7), (5, 7, 7),
	                                 (5, 3, 6), (5, 4, 6), (5, 5, 6), (5, 6, 6), (5, 7, 6),
	                      (5, 2, 5), (5, 3, 5), (5, 4, 5), (5, 5, 5), (5, 6, 5), (5, 7, 5),
	           (5, 1, 4), (5, 2, 4), (5, 3, 4), (5, 4, 4), (5, 5, 4), (5, 6, 4), (5, 7, 4),
	(5, 0, 3), (5, 1, 3), (5, 2, 3), (5, 3, 3), (5, 4, 3), (5, 5, 3), (5, 6, 3), (5, 7, 3),
	(5, 0, 2), (5, 1, 2), (5, 2, 2), (5, 3, 2), (5, 4, 2), (5, 5, 2), (5, 6, 2),
	(5, 0, 1), (5, 1, 1), (5, 2, 1), (5, 3, 1), (5, 4, 1), (5, 5, 1),
	(5, 0, 0), (5, 1, 0), (5, 2, 0), (5, 3, 0), (5, 4, 0);

-- Create boards rarely seen in the wild
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 2, chip_x, chip_y FROM board_model_coords WHERE model = 3;
INSERT OR IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 4, chip_x, chip_y FROM board_model_coords WHERE model = 5;

-- Standard directions between boards
INSERT OR IGNORE INTO directions("id", name)
VALUES
	(0, 'N'), (1, 'NE'), (2, 'SE'), (3, 'S'), (4, 'SW'), (5, 'NW');

INSERT OR IGNORE INTO movement_directions(z, direction, dx, dy, dz)
VALUES
	-- Z = 0
	(0, 0, 0, 0, +2),
	(0, 1, 0, 0, +1),
	(0, 2, 0, -1, +2),
	(0, 3, -1, -1, +1),
	(0, 4, -1, -1, +2),
	(0, 5, -1, 0, +1),
	-- Z = 1
	(1, 0, +1, +1, -1),
	(1, 1, +1, 0, +1),
	(1, 2, +1, 0, -1),
	(1, 3, 0, -1, +1),
	(1, 4, 0, 0, -1),
	(1, 5, 0, 0, +1),
	-- Z = 2
	(2, 0, 0, +1, -1),
	(2, 1, +1, +1, -2),
	(2, 2, 0, 0, -1),
	(2, 3, 0, 0, -2),
	(2, 4, -1, 0, -1),
	(2, 5, 0, +1, -2);

COMMIT;

-- Lock down the board_model_coords table
CREATE TRIGGER IF NOT EXISTS board_layout_is_static_no_update
BEFORE UPDATE ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS board_layout_is_static_no_insert
BEFORE INSERT ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS board_layout_is_static_no_delete
BEFORE DELETE ON board_model_coords
BEGIN
    SELECT RAISE(IGNORE);
END;

-- Lock down the movement directions
CREATE TRIGGER IF NOT EXISTS directions_is_static_no_update
BEFORE UPDATE ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS directions_is_static_no_insert
BEFORE INSERT ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS directions_is_static_no_delete
BEFORE DELETE ON directions
BEGIN
	SELECT RAISE(IGNORE);
END;

-- Lock down the inter-board movement patterns
CREATE TRIGGER IF NOT EXISTS movement_directions_is_static_no_update
BEFORE UPDATE ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS movement_directions_is_static_no_insert
BEFORE INSERT ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS movement_directions_is_static_no_delete
BEFORE DELETE ON movement_directions
BEGIN
	SELECT RAISE(IGNORE);
END;

-- Automatically suggested indices
CREATE INDEX IF NOT EXISTS 'boards_allocated_job' ON 'boards'('allocated_job'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'boards_machine_id' ON 'boards'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'job_request_job_id' ON 'job_request'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'jobs_root_id' ON 'jobs'('root_id'); --> boards(board_id)
CREATE INDEX IF NOT EXISTS 'jobs_machine_id' ON 'jobs'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'movement_directions_direction' ON 'movement_directions'('direction'); --> directions(id)
CREATE INDEX IF NOT EXISTS 'pending_changes_job_id' ON 'pending_changes'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'tags_machine_id' ON 'tags'('machine_id'); --> machines(machine_id)
