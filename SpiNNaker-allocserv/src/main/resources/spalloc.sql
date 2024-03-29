-- Copyright (c) 2021 The University of Manchester
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

CREATE TABLE IF NOT EXISTS directions(
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS movement_directions(
	z INTEGER NOT NULL,
	direction INTEGER NOT NULL
		CONSTRAINT "movement_directions.direction -> directions.id"
		REFERENCES directions("id") ON DELETE RESTRICT,
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

CREATE TABLE IF NOT EXISTS board_models (
	model INTEGER PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS group_types (
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS machines (
	machine_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_name TEXT UNIQUE NOT NULL
		CONSTRAINT "machine_name non-evil" CHECK (
			machine_name NOT LIKE '%{%' AND machine_name NOT LIKE '%}%'),
	width INTEGER NOT NULL
		CONSTRAINT "machines.width sane" CHECK (width > 0),
	height INTEGER NOT NULL
		CONSTRAINT "machines.height sane" CHECK (height > 0),
	"depth" INTEGER NOT NULL
		CONSTRAINT "machines.depth sane" CHECK ("depth" IN (1, 3)),
	board_model INTEGER NOT NULL
		CONSTRAINT "machines.board_model -> board_models.model"
		REFERENCES board_models(model) ON DELETE CASCADE,
	-- Minimum times (in seconds) to wait after switching a board on or off
	-- before it can have its power state changed again.
	on_delay INTEGER NOT NULL DEFAULT (20), -- after on delay
	off_delay INTEGER NOT NULL DEFAULT (30), -- after off delay
	default_quota INTEGER, -- NULL for no quota
	max_chip_x INTEGER NOT NULL DEFAULT (0),
	max_chip_y INTEGER NOT NULL DEFAULT (0),
	in_service INTEGER NOT NULL DEFAULT (1)
		CONSTRAINT "machines.in_service bool" CHECK (in_service IN (0, 1))
);
CREATE TABLE IF NOT EXISTS tags (
	machine_id INTEGER NOT NULL
		CONSTRAINT "tags.machine_id -> machines.machine_id"
		REFERENCES machines(machine_id) ON DELETE CASCADE,
	tag TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS boards (
	board_id INTEGER PRIMARY KEY AUTOINCREMENT,
	address TEXT UNIQUE, -- IP address
	bmp_id INTEGER NOT NULL
		CONSTRAINT "boards.bmp_id -> bmp.bmp_id"
		REFERENCES bmp(bmp_id) ON DELETE RESTRICT,
	board_num INTEGER
		CONSTRAINT "boards.board_num sane" CHECK (board_num >= 0), -- for use with the BMP
	machine_id INTEGER NOT NULL
		CONSTRAINT "boards.machine_id -> machines.machine_id"
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	x INTEGER NOT NULL
		CONSTRAINT "boards.x sane" CHECK (x >= 0), -- Board logical coordinate
	y INTEGER NOT NULL
		CONSTRAINT "boards.y sane" CHECK (y >= 0), -- Board logical coordinate
	z INTEGER NOT NULL
		CONSTRAINT "boards.z sane" CHECK (z >= 0), -- Board logical coordinate
	root_x INTEGER NOT NULL
		CONSTRAINT "boards.root_x sane" CHECK (root_x >= 0), -- Chip coordinate
	root_y INTEGER NOT NULL
		CONSTRAINT "boards.root_y sane" CHECK (root_y >= 0), -- Chip coordinate
	allocated_job INTEGER
		CONSTRAINT "boards.allocated_job -> jobs.job_id"
		REFERENCES jobs(job_id),
	board_power INTEGER
		CONSTRAINT "boards.board_power bool" CHECK (board_power IN (0, 1)),
	power_off_timestamp INTEGER, -- timestamp
	power_on_timestamp INTEGER, -- timestamp
	functioning INTEGER, -- boolean
	blacklist_set_timestamp INTEGER DEFAULT (0), -- timestamp
	blacklist_sync_timestamp INTEGER DEFAULT (0), -- timestamp
	may_be_allocated INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		board_num IS NOT NULL
		AND allocated_job IS NULL
		AND (functioning IS NULL OR functioning != 0)
		) VIRTUAL,
	-- Ether both address and board_num are NULL, or neither is
	CONSTRAINT "boards: address and board_num sane together" CHECK (
		(address IS NULL) = (board_num IS NULL))
);
-- Every board has a unique location within its machine
CREATE UNIQUE INDEX IF NOT EXISTS boardCoordinateSanity ON boards(
	root_x ASC, root_y ASC, machine_id ASC);
-- Every board has a unique board number within its BMP
CREATE UNIQUE INDEX IF NOT EXISTS boardBmpSanity ON boards(
	bmp_id ASC, board_num ASC);
CREATE INDEX IF NOT EXISTS boardReuseTimestamps ON boards(
	power_off_timestamp ASC);
-- When the power is changed, update the right timestamp and deallocate if necessary
CREATE TRIGGER IF NOT EXISTS boardStateTimestamping
AFTER UPDATE OF board_power ON boards
BEGIN
	UPDATE boards
		SET power_off_timestamp = CAST(strftime('%s','now') AS INTEGER)
		WHERE board_id = NEW.board_id AND OLD.board_power IS NOT 0 AND NEW.board_power IS 0;
	UPDATE boards
		SET power_on_timestamp = CAST(strftime('%s','now') AS INTEGER)
		WHERE board_id = NEW.board_id AND OLD.board_power IS NOT 1 AND NEW.board_power IS 1;
END;

CREATE TABLE IF NOT EXISTS board_serial (
	bs_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER UNIQUE NOT NULL
		CONSTRAINT "board_serial.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	bmp_serial_id TEXT UNIQUE,
	physical_serial_id TEXT UNIQUE
);

-- These are the *DIRECTLY* addressible BMPs, one per frame
CREATE TABLE IF NOT EXISTS bmp (
	bmp_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER NOT NULL
		CONSTRAINT "bmp.machine_id -> machines.machine_id"
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	address TEXT UNIQUE NOT NULL, -- IP address
	cabinet INTEGER NOT NULL
		CONSTRAINT "bmp.cabinet sane" CHECK (cabinet >= 0),
	frame INTEGER NOT NULL
		CONSTRAINT "bmp.frame sane" CHECK (frame >= 0)
);
-- Every BMP has a unique location within its machine
CREATE UNIQUE INDEX IF NOT EXISTS bmpSanity ON bmp(
    machine_id, cabinet, frame
);

CREATE TABLE IF NOT EXISTS links (
	link_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_1 INTEGER NOT NULL
		CONSTRAINT "links.board_1 -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_1 INTEGER NOT NULL
		CONSTRAINT "links.dir_1 -> directions.id"
		REFERENCES directions(id) ON DELETE RESTRICT,
	board_2 INTEGER NOT NULL
		CONSTRAINT "links.board_2 -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_2 INTEGER NOT NULL
		CONSTRAINT "links.dir_2 -> directions.id"
		REFERENCES directions(id) ON DELETE RESTRICT,
	live INTEGER NOT NULL DEFAULT (1)
		CONSTRAINT "links.live bool" CHECK (live IN (0, 1)),
	CONSTRAINT "links ordering sane" CHECK (board_1 <= board_2)
);
CREATE UNIQUE INDEX IF NOT EXISTS link_1 ON links(
	board_1 ASC, dir_1 ASC);
CREATE UNIQUE INDEX IF NOT EXISTS link_2 ON links(
	board_2 ASC, dir_2 ASC);

-- Reports of problems with boards
CREATE TABLE IF NOT EXISTS board_reports(
	report_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER NOT NULL
		CONSTRAINT "board_reports.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	job_id INTEGER
		CONSTRAINT "board_reports.job_id -> jobs.job_id"
		REFERENCES jobs(job_id),
	reported_issue TEXT NOT NULL,
	reporter INTEGER NOT NULL
		CONSTRAINT "board_reports.reporter -> user_info.user_id"
		REFERENCES user_info(user_id) ON DELETE RESTRICT,
	report_timestamp INTEGER	-- automatically timestamped
);
CREATE UNIQUE INDEX IF NOT EXISTS board_reports_sanity ON board_reports(
	board_id ASC, job_id ASC, reporter ASC);

-- When the issue report is created, update the right timestamp
CREATE TRIGGER IF NOT EXISTS boardReportsTimestamping
AFTER INSERT ON board_reports
BEGIN
	UPDATE board_reports
		SET report_timestamp = CAST(strftime('%s','now') AS INTEGER)
	WHERE report_id = NEW.report_id;
END;

CREATE TABLE IF NOT EXISTS job_states(
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS jobs (
	job_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER
		CONSTRAINT "jobs.machine_id -> machines.machine_id"
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	owner INTEGER NOT NULL
		CONSTRAINT "jobs.owner -> user_info.user_id"
		REFERENCES user_info(user_id) ON DELETE RESTRICT,
	create_timestamp INTEGER NOT NULL,
	width INTEGER				-- set after allocation, in triads
		CONSTRAINT "jobs.width sane" CHECK (width > 0),
	height INTEGER				-- set after allocation, in triads
		CONSTRAINT "jobs.height sane" CHECK (height > 0),
	"depth" INTEGER				-- set after allocation
		CONSTRAINT "jobs.depth sane" CHECK ("depth" > 0),
	root_id INTEGER				-- set after allocation
		CONSTRAINT "jobs.root_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE RESTRICT,
	job_state INTEGER NOT NULL
		CONSTRAINT "jobs.job_state -> job_states.id"
		REFERENCES job_states(id) ON DELETE RESTRICT,
	keepalive_interval INTEGER NOT NULL,
	keepalive_timestamp INTEGER, -- timestamp
	keepalive_host TEXT,		-- IP address
	death_reason TEXT,
	death_timestamp INTEGER,	-- timestamp; set by trigger
	original_request BLOB,		-- Stores it, but doesn't otherwise care
	num_pending INTEGER NOT NULL DEFAULT (0),
	allocation_timestamp INTEGER, -- timestamp; set by trigger
	allocation_size INTEGER,
	allocated_root INTEGER,		-- set by trigger
	accounted_for INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "jobs.accounted_for bool" CHECK (accounted_for IN (0, 1)),
	group_id INTEGER NOT NULL
		CONSTRAINT "jobs.group_id -> groups.group_id"
		REFERENCES groups(group_id) ON DELETE RESTRICT
	-- We do not check that the user is necessarily still a member of the group;
	-- that's only a check carried out by the application on job creation. This
	-- is *by design*; users may leave groups, but their jobs do not.
);

CREATE VIEW IF NOT EXISTS jobs_usage(
	machine_id, job_id, owner, group_id, quota, "size", "start", "finish",
	"duration", "usage", "complete") AS
SELECT
	machine_id, job_id, owner, group_id, groups.quota, allocation_size,
	allocation_timestamp, death_timestamp,
	COALESCE(
		COALESCE(death_timestamp, CAST(strftime('%s','now') AS INTEGER)) - allocation_timestamp,
		0),
	COALESCE(allocation_size, 0) * COALESCE(
		COALESCE(death_timestamp, CAST(strftime('%s','now') AS INTEGER)) - allocation_timestamp,
		0),
	job_state = 4 -- DESTROYED
FROM jobs JOIN groups USING (group_id)
WHERE NOT accounted_for;

-- When the job is created, update the right timestamp
CREATE TRIGGER IF NOT EXISTS jobCreateTimestamping
AFTER INSERT ON jobs
BEGIN
	UPDATE jobs
		SET create_timestamp = CAST(strftime('%s','now') AS INTEGER)
	WHERE job_id = NEW.job_id;
END;
-- When the job is allocated, update the right timestamp
CREATE TRIGGER IF NOT EXISTS jobAllocationTimestamping
AFTER UPDATE OF allocation_size ON jobs
BEGIN
	UPDATE jobs
		SET allocation_timestamp = CAST(strftime('%s','now') AS INTEGER)
	WHERE job_id = OLD.job_id
		AND (OLD.allocation_size IS NULL OR OLD.allocation_size = 0);
END;
-- When the job is destroyed, update the right timestamp
CREATE TRIGGER IF NOT EXISTS jobDeathTimestamping
AFTER UPDATE OF job_state ON jobs
BEGIN
	UPDATE jobs
		SET death_timestamp = CAST(strftime('%s','now') AS INTEGER)
	WHERE job_id = NEW.job_id AND OLD.job_state IS NOT 4 -- DESTROYED
		AND NEW.job_state IS 4; -- DESTROYED
END;
-- When a job is given a root board, remember that permanently
CREATE TRIGGER IF NOT EXISTS jobAllocationRememberRoot
AFTER UPDATE OF root_id ON jobs
BEGIN
	UPDATE jobs
		SET allocated_root = NEW.root_id
	WHERE job_id = OLD.job_id AND NEW.root_id IS NOT NULL;
END;

-- When the power is turned off on a board allocated to a job that is destroyed,
-- deallocate the board.
CREATE TRIGGER IF NOT EXISTS boardDeallocation
AFTER UPDATE OF board_power ON boards
BEGIN
	UPDATE boards
		SET allocated_job = NULL
	WHERE board_id = NEW.board_id
		AND OLD.board_power IS NOT 0 AND NEW.board_power IS 0
		AND EXISTS(
			SELECT 1 FROM jobs
			WHERE jobs.job_id = OLD.allocated_job AND jobs.job_state = 4);
END;

-- Record all allocations
CREATE TABLE IF NOT EXISTS old_board_allocations (
	alloc_id INTEGER PRIMARY KEY AUTOINCREMENT,
	job_id INTEGER NOT NULL
		CONSTRAINT "old_board_allocations.job_id -> jobs.job_id"
		REFERENCES jobs(job_id) ON DELETE CASCADE,
	board_id INTEGER NOT NULL
		CONSTRAINT "old_board_allocations.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	alloc_timestamp INTEGER NOT NULL
);
CREATE TRIGGER IF NOT EXISTS alloc_tracking
AFTER UPDATE OF allocated_job ON boards WHEN NEW.allocated_job IS NOT NULL
BEGIN
	INSERT OR IGNORE INTO old_board_allocations(
		job_id, board_id, alloc_timestamp)
	VALUES (NEW.allocated_job, NEW.board_id,
		CAST(strftime('%s','now') AS INTEGER));
END;

CREATE TABLE IF NOT EXISTS job_request (
	req_id INTEGER PRIMARY KEY AUTOINCREMENT,
	job_id INTEGER NOT NULL
		CONSTRAINT "job_request.job_id -> jobs.job_id"
		REFERENCES jobs(job_id) ON DELETE CASCADE,
	num_boards INTEGER
		CONSTRAINT "job_request.num_boards sane" CHECK (num_boards > 0),
	width INTEGER -- in triads
		CONSTRAINT "job_request.width sane" CHECK (width > 0),
	height INTEGER -- in triads
		CONSTRAINT "job_request.height sane" CHECK (height > 0),
	board_id INTEGER
		CONSTRAINT "job_request.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	max_dead_boards INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "job_request.max_dead_boards sane" CHECK (max_dead_boards >= 0),
	importance INTEGER NOT NULL DEFAULT (0),
	priority INTEGER NOT NULL DEFAULT (1)
);
CREATE INDEX IF NOT EXISTS job_request_importance ON job_request(
	importance DESC, job_id ASC);

CREATE TABLE IF NOT EXISTS pending_changes (
    change_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER
		CONSTRAINT "pending_changes.job_id -> jobs.job_id"
		REFERENCES jobs(job_id) ON DELETE CASCADE,
    board_id INTEGER UNIQUE NOT NULL
		CONSTRAINT "pending_changes.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE RESTRICT,
    "power" INTEGER NOT NULL	-- Whether to switch the board on
		CONSTRAINT "pending_changes.power bool" CHECK ("power" IN (0, 1)),
    fpga_n INTEGER NOT NULL		-- Whether to switch the northward FPGA on
		CONSTRAINT "pending_changes.fpga_n bool" CHECK (fpga_n IN (0, 1)),
    fpga_s INTEGER NOT NULL		-- Whether to switch the southward FPGA on
		CONSTRAINT "pending_changes.fpga_s bool" CHECK (fpga_s IN (0, 1)),
    fpga_e INTEGER NOT NULL		-- Whether to switch the eastward FPGA on
		CONSTRAINT "pending_changes.fpga_e bool" CHECK (fpga_e IN (0, 1)),
    fpga_w INTEGER NOT NULL		-- Whether to switch the westward FPGA on
		CONSTRAINT "pending_changes.fpga_w bool" CHECK (fpga_w IN (0, 1)),
    fpga_nw INTEGER NOT NULL	-- Whether to switch the northwest FPGA on
		CONSTRAINT "pending_changes.fpga_nw bool" CHECK (fpga_nw IN (0, 1)),
    fpga_se INTEGER NOT NULL	-- Whether to switch the southeast FPGA on
		CONSTRAINT "pending_changes.fpga_se bool" CHECK (fpga_se IN (0, 1)),
    in_progress INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "pending_changes.in_progress bool" CHECK (in_progress IN (0, 1)),
    from_state INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "pending_changes.from_state -> job_states.id"
		REFERENCES job_states(id) ON DELETE RESTRICT,
    to_state INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "pending_changes.to_state -> job_states.id"
		REFERENCES job_states(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS blacklist_ops (
	op_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER UNIQUE NOT NULL
		CONSTRAINT "blacklist_ops.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE RESTRICT,
	op INTEGER NOT NULL,		-- What we plan to do (NonBootOperations ID)
	"data" BLOB,				-- The serialized blacklist info; JAVA format!
	completed INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "blacklist_ops.completed bool" CHECK (completed IN (0, 1)),
	failure BLOB				-- The serialized exception on failure; JAVA format!
);

-- Coordinates of chips in a board
CREATE TABLE IF NOT EXISTS board_model_coords(
	coord_id INTEGER PRIMARY KEY AUTOINCREMENT,
	model INTEGER NOT NULL
		CONSTRAINT "board_model_coords.model -> board_models.model"
		REFERENCES board_models(model) ON DELETE CASCADE,
	chip_x INTEGER NOT NULL
		CONSTRAINT "board_model_coords.chip_x sane" CHECK (chip_x >= 0),
	chip_y INTEGER NOT NULL
		CONSTRAINT "board_model_coords.chip_y sane" CHECK (chip_y >= 0)
);
-- Board chip descriptors are unique
CREATE UNIQUE INDEX IF NOT EXISTS chipUniqueness ON board_model_coords(
    model, chip_x, chip_y
);

-- Per-chip blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_chips(
	blacklist_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_chips.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_chips.coord_id -> board_model_coords.coord_id"
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	notes TEXT);
CREATE UNIQUE INDEX IF NOT EXISTS blacklisted_chips_sanity on blacklisted_chips(
	board_id ASC, coord_id ASC);

-- Per-physical-core blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_cores(
	blacklist_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_cores.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_cores.coord_id -> board_model_coords.coord_id"
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	physical_core INTEGER NOT NULL
		CONSTRAINT "blacklisted_cores.physical_core sane" CHECK (physical_core >= 0),
	notes TEXT);
CREATE UNIQUE INDEX IF NOT EXISTS blacklisted_cores_sanity on blacklisted_cores(
	board_id ASC, coord_id ASC, physical_core ASC);

-- Per-link blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_links(
	blacklist_id INTEGER PRIMARY KEY AUTOINCREMENT,
	board_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_links.board_id -> boards.board_id"
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL
		CONSTRAINT "blacklisted_links.coord_id -> board_model_coords.coord_id"
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	direction INTEGER NOT NULL
		CONSTRAINT "blacklisted_links.direction -> directions.id"
		REFERENCES directions("id") ON DELETE RESTRICT,
	notes TEXT);
CREATE UNIQUE INDEX IF NOT EXISTS blacklisted_links_sanity on blacklisted_links(
	board_id ASC, coord_id ASC, direction ASC);

CREATE TABLE IF NOT EXISTS user_info (
	user_id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_name TEXT UNIQUE NOT NULL,
	encrypted_password TEXT, -- If NULL, login via OpenID
	last_successful_login_timestamp INTEGER, -- If NULL, never logged in
	-- Roles; see SecurityConfig.TrustLevel
	trust_level INTEGER NOT NULL
		CONSTRAINT "user_info.trust_level sane" CHECK (trust_level IN (0, 1, 2, 3)),
	-- Automatic disablement support
	failure_count INTEGER NOT NULL DEFAULT (0),
	locked INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "user_info.locked bool" CHECK (locked IN (0, 1)),
	last_fail_timestamp INTEGER NOT NULL DEFAULT (0),
	-- Administrative disablement support
	disabled INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "user_info.disabled bool" CHECK (disabled IN (0, 1)),
	openid_subject TEXT UNIQUE,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		encrypted_password IS NOT NULL) VIRTUAL
);

CREATE TABLE IF NOT EXISTS groups (
	group_id INTEGER PRIMARY KEY AUTOINCREMENT,
	group_name TEXT UNIQUE NOT NULL,
	quota INTEGER,				-- If NULL, no quota applies; care required, could be LARGE
	group_type INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT "groups.group_type -> group_types.id"
		REFERENCES group_types(id) ON DELETE RESTRICT,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		group_type = 0) VIRTUAL
);

-- Many-to-many relationship model
CREATE TABLE IF NOT EXISTS group_memberships (
	membership_id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id INTEGER NOT NULL
		CONSTRAINT "group_memberships.user_id -> user_info.user_id"
		REFERENCES user_info(user_id) ON DELETE CASCADE,
	group_id INTEGER NOT NULL
		CONSTRAINT "group_memberships.group_id -> groups.group_id"
		REFERENCES groups(group_id) ON DELETE CASCADE
);
-- No user can be in a group more than once!
CREATE UNIQUE INDEX IF NOT EXISTS membershipSanity ON group_memberships(
	user_id, group_id
);
-- Internal users can only be in internal groups.
-- External (OIDC) users can only be in external groups.
CREATE TRIGGER IF NOT EXISTS userGroupSanity
AFTER INSERT ON group_memberships
WHEN
	(SELECT user_info.is_internal FROM user_info
		WHERE user_info.user_id = NEW.user_id) != (
	SELECT groups.is_internal FROM groups
		WHERE groups.group_id = NEW.group_id)
BEGIN
	SELECT RAISE(FAIL, 'group and user type don''t match');
END;

-- Simulate legacy view
CREATE VIEW IF NOT EXISTS quotas (quota_id, user_id, machine_id, quota)
AS SELECT
	groups.group_id, user_info.user_id, machines.machine_id, groups.quota
FROM groups LEFT JOIN group_memberships USING (group_id)
LEFT JOIN user_info USING (user_id)
LEFT JOIN machines;

-- Automatically suggested indices
CREATE INDEX IF NOT EXISTS 'boards_allocated_job' ON 'boards'('allocated_job'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'boards_machine_id' ON 'boards'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'job_request_job_id' ON 'job_request'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'jobs_root_id' ON 'jobs'('root_id'); --> boards(board_id)
CREATE INDEX IF NOT EXISTS 'jobs_machine_id' ON 'jobs'('machine_id'); --> machines(machine_id)
CREATE INDEX IF NOT EXISTS 'movement_directions_direction' ON 'movement_directions'('direction'); --> directions(id)
CREATE INDEX IF NOT EXISTS 'pending_changes_job_id' ON 'pending_changes'('job_id'); --> jobs(job_id)
CREATE INDEX IF NOT EXISTS 'tags_machine_id' ON 'tags'('machine_id'); --> machines(machine_id)
