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
	width INTEGER NOT NULL CHECK (width > 0),
	height INTEGER NOT NULL CHECK (height > 0),
	"depth" INTEGER NOT NULL CHECK ("depth" IN (1, 3)),
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
	board_num INTEGER NOT NULL CHECK (board_num >= 0), -- for use with the BMP
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE RESTRICT,
	x INTEGER NOT NULL CHECK (x >= 0), -- Board logical coordinate
	y INTEGER NOT NULL CHECK (y >= 0), -- Board logical coordinate
	z INTEGER NOT NULL CHECK (z >= 0), -- Board logical coordinate
	root_x INTEGER NOT NULL CHECK (root_x >= 0), -- Chip coordinate
	root_y INTEGER NOT NULL CHECK (root_y >= 0), -- Chip coordinate
	allocated_job INTEGER REFERENCES jobs(job_id),
	board_power INTEGER CHECK (board_power IN (0, 1)),
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
CREATE INDEX IF NOT EXISTS boardReuseTimestamps ON boards(
	power_off_timestamp ASC);
-- When the power is changed, update the right timestamp
CREATE TRIGGER IF NOT EXISTS boardStateTimestamping
AFTER UPDATE OF board_power ON boards
BEGIN
	UPDATE boards
		SET power_off_timestamp = strftime('%s','now')
		WHERE board_id = NEW.board_id AND OLD.board_power IS NOT 0 AND NEW.board_power IS 0;
	UPDATE boards
		SET power_on_timestamp = strftime('%s','now')
		WHERE board_id = NEW.board_id AND OLD.board_power IS NOT 1 AND NEW.board_power IS 1;
END;

CREATE TABLE IF NOT EXISTS bmp (
	bmp_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER NOT NULL REFERENCES machines(machine_id) ON DELETE RESTRICT,
	address TEXT UNIQUE NOT NULL, -- IP address
	cabinet INTEGER NOT NULL CHECK (cabinet >= 0),
	frame INTEGER NOT NULL CHECK (frame >= 0)
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
	live INTEGER NOT NULL DEFAULT (1) CHECK (live IN (0, 1)),
	CHECK (board_1 <= board_2)
);
CREATE UNIQUE INDEX IF NOT EXISTS link_1 ON links(
	board_1 ASC, dir_1 ASC);
CREATE UNIQUE INDEX IF NOT EXISTS link_2 ON links(
	board_2 ASC, dir_2 ASC);
CREATE UNIQUE INDEX IF NOT EXISTS only_one_link_between_boards ON links(
	board_1 ASC, board_2 ASC);

CREATE TABLE IF NOT EXISTS job_states(
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS jobs (
	job_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER REFERENCES machines(machine_id) ON DELETE RESTRICT,
	owner TEXT NOT NULL,
	create_timestamp INTEGER NOT NULL,
	width INTEGER CHECK (width > 0), -- set after allocation
	height INTEGER CHECK (height > 0), -- set after allocation
	"depth" INTEGER CHECK ("depth" > 0), -- set after allocation
	root_id INTEGER REFERENCES boards(board_id) ON DELETE RESTRICT, -- set after allocation
	job_state INTEGER NOT NULL REFERENCES job_states(id) ON DELETE RESTRICT,
	keepalive_interval INTEGER NOT NULL,
	keepalive_timestamp INTEGER, -- timestamp
	keepalive_host TEXT, -- IP address
	death_reason TEXT,
	death_timestamp INTEGER, -- timestamp
	original_request BLOB, -- Stores it, but doesn't otherwise care
	num_pending INTEGER NOT NULL DEFAULT (0)
);
-- When the job is created, update the right timestamp
CREATE TRIGGER IF NOT EXISTS jobCreateTimestamping
AFTER INSERT ON jobs
BEGIN
	UPDATE jobs
		SET create_timestamp = strftime('%s','now')
		WHERE job_id = NEW.job_id;
END;
-- When the job is destroyed, update the right timestamp
CREATE TRIGGER IF NOT EXISTS jobDeathTimestamping
AFTER UPDATE OF job_state ON jobs
BEGIN
	UPDATE jobs
		SET death_timestamp = strftime('%s','now')
		WHERE job_id = NEW.job_id AND OLD.job_state IS NOT 4 AND NEW.job_state IS 4;
END;

CREATE TABLE IF NOT EXISTS job_request (
	req_id INTEGER PRIMARY KEY AUTOINCREMENT,
	job_id INTEGER NOT NULL REFERENCES jobs(job_id) ON DELETE CASCADE,
	num_boards INTEGER CHECK (num_boards > 0),
	width INTEGER CHECK (width > 0),
	height INTEGER CHECK (height > 0),
	x INTEGER CHECK (x >= 0),
	y INTEGER CHECK (y >= 0),
	z INTEGER CHECK (z >= 0),
	max_dead_boards INTEGER NOT NULL DEFAULT (0) CHECK (max_dead_boards >= 0)
);

CREATE TABLE IF NOT EXISTS pending_changes (
    change_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER REFERENCES jobs(job_id) ON DELETE CASCADE,
    board_id INTEGER UNIQUE NOT NULL REFERENCES boards(board_id) ON DELETE RESTRICT,
    "power" INTEGER NOT NULL CHECK ("power" IN (0, 1)), -- Whether to switch the board on
    fpga_n INTEGER NOT NULL CHECK (fpga_n IN (0, 1)), -- Whether to switch the northward FPGA on
    fpga_s INTEGER NOT NULL CHECK (fpga_s IN (0, 1)), -- Whether to switch the southward FPGA on
    fpga_e INTEGER NOT NULL CHECK (fpga_e IN (0, 1)), -- Whether to switch the eastward FPGA on
    fpga_w INTEGER NOT NULL CHECK (fpga_w IN (0, 1)), -- Whether to switch the westward FPGA on
    fpga_nw INTEGER NOT NULL CHECK (fpga_nw IN (0, 1)), -- Whether to switch the northwest FPGA on
    fpga_se INTEGER NOT NULL CHECK (fpga_se IN (0, 1)), -- Whether to switch the southeast FPGA on
    in_progress INTEGER NOT NULL DEFAULT (0) CHECK (in_progress IN (0, 1)),
    from_state INTEGER NOT NULL DEFAULT (0) REFERENCES job_states(id) ON DELETE RESTRICT,
    to_state INTEGER NOT NULL DEFAULT (0) REFERENCES job_states(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS board_model_coords(
	-- We never need the identitites of the rows
	model INTEGER NOT NULL CHECK (model IN (2, 3, 4, 5)),
	chip_x INTEGER NOT NULL CHECK (chip_x >= 0),
	chip_y INTEGER NOT NULL CHECK (chip_y >= 0)
);
-- Board chip descriptors are unique
CREATE UNIQUE INDEX IF NOT EXISTS chipUniqueness ON board_model_coords(
    model, chip_x, chip_y
);

-- Automatically suggested indices
CREATE INDEX IF NOT EXISTS 'boards_allocated_job' ON 'boards'('allocated_job'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'boards_machine_id' ON 'boards'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'job_request_job_id' ON 'job_request'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'jobs_root_id' ON 'jobs'('root_id'); --> boards(board_id)
CREATE INDEX IF NOT EXISTS 'jobs_machine_id' ON 'jobs'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'movement_directions_direction' ON 'movement_directions'('direction'); --> directions(id)
CREATE INDEX IF NOT EXISTS 'pending_changes_job_id' ON 'pending_changes'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'tags_machine_id' ON 'tags'('machine_id'); --> machines(machine_id)
