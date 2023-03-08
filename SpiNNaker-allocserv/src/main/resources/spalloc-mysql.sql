-- Copyright (c) 2021-2022 The University of Manchester
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

-- STMT Marks the gaps between actual statements, so these can be parsed in Java
-- IGNORE Means this bit will be ignored until the next -- STMT

-- STMT
CREATE TABLE IF NOT EXISTS directions(
	id INTEGER PRIMARY KEY,
	name CHAR(50) UNIQUE NOT NULL
);

-- STMT
CREATE TABLE IF NOT EXISTS movement_directions(
	z INTEGER NOT NULL,
	direction INTEGER NOT NULL,
	dx INTEGER NOT NULL,
	dy INTEGER NOT NULL,
	dz INTEGER NOT NULL,
	FOREIGN KEY (direction) REFERENCES directions(id),
	UNIQUE INDEX movementUniqueness (z ASC, direction ASC),
	INDEX (direction)
);

-- STMT
CREATE OR REPLACE VIEW motions AS
SELECT z, directions.name AS dir, dx, dy, dz
FROM movement_directions JOIN directions
	ON movement_directions.direction = directions.id;

-- STMT
CREATE TABLE IF NOT EXISTS board_models (
	model INTEGER PRIMARY KEY
);

-- STMT
CREATE TABLE IF NOT EXISTS group_types (
	id INTEGER PRIMARY KEY,
	name CHAR(50) UNIQUE NOT NULL
);

-- STMT
CREATE TABLE IF NOT EXISTS machines (
	machine_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	machine_name CHAR(50) UNIQUE NOT NULL
		CONSTRAINT CHECK (
			machine_name NOT LIKE '%{%' AND machine_name NOT LIKE '%}%'),
	width INTEGER NOT NULL
		CONSTRAINT width_check CHECK (width > 0),
	height INTEGER NOT NULL
		CONSTRAINT height_check CHECK (height > 0),
	depth INTEGER NOT NULL
		CONSTRAINT depth_check CHECK (depth IN (1, 3)),
	board_model INTEGER NOT NULL,
		FOREIGN KEY (board_model) REFERENCES board_models(model) ON DELETE CASCADE,
	-- Minimum times (in seconds) to wait after switching a board on or off
	-- before it can have its power state changed again.
	on_delay INTEGER NOT NULL DEFAULT (20), -- after on delay
	off_delay INTEGER NOT NULL DEFAULT (30), -- after off delay
	default_quota INTEGER, -- NULL for no quota
	max_chip_x INTEGER NOT NULL DEFAULT (0),
	max_chip_y INTEGER NOT NULL DEFAULT (0),
	in_service INTEGER NOT NULL DEFAULT (1)
		CONSTRAINT in_service_check CHECK (in_service IN (0, 1))
);

-- STMT
CREATE TABLE IF NOT EXISTS tags (
	machine_id INTEGER NOT NULL,
		FOREIGN KEY (machine_id)
		REFERENCES machines(machine_id) ON DELETE CASCADE,
	tag CHAR(50) NOT NULL,
	INDEX (machine_id)
);

-- STMT
-- These are the *DIRECTLY* addressible BMPs, one per frame
CREATE TABLE IF NOT EXISTS bmp (
	bmp_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	machine_id INTEGER NOT NULL,
		FOREIGN KEY (machine_id)
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	address CHAR(50) UNIQUE NOT NULL, -- IP address
	cabinet INTEGER NOT NULL
		CONSTRAINT cabinet_sane CHECK (cabinet >= 0),
	frame INTEGER NOT NULL
		CONSTRAINT frame_sane CHECK (frame >= 0),
	UNIQUE INDEX (machine_id, cabinet, frame)
);

-- STMT
CREATE TABLE IF NOT EXISTS boards (
	board_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	address CHAR(50) UNIQUE, -- IP address
	bmp_id INTEGER NOT NULL,
		FOREIGN KEY (bmp_id)
		REFERENCES bmp(bmp_id) ON DELETE RESTRICT,
	board_num INTEGER
		CONSTRAINT board_check CHECK (board_num >= 0), -- for use with the BMP
	machine_id INTEGER NOT NULL,
		FOREIGN KEY (machine_id)
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	x INTEGER NOT NULL
		CONSTRAINT x_sane CHECK (x >= 0), -- Board logical coordinate
	y INTEGER NOT NULL
		CONSTRAINT y_sane CHECK (y >= 0), -- Board logical coordinate
	z INTEGER NOT NULL
		CONSTRAINT z_sane CHECK (z >= 0), -- Board logical coordinate
	root_x INTEGER NOT NULL
		CONSTRAINT root_x_sane CHECK (root_x >= 0), -- Chip coordinate
	root_y INTEGER NOT NULL
		CONSTRAINT root_y_sane CHECK (root_y >= 0), -- Chip coordinate
	allocated_job INTEGER,
	    -- job table doesn't exist, so we can't reference it
		-- FOREIGN KEY (allocated_job) REFERENCES jobs(job_id),
	board_power INTEGER
		CONSTRAINT board_power_check CHECK (board_power IN (0, 1)),
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
	CONSTRAINT address_and_num_sane CHECK (
		(address IS NULL) = (board_num IS NULL)),
	-- Every board has a unique location within its machine
	UNIQUE INDEX (root_x ASC, root_y ASC, machine_id ASC),
	-- Every board has a unique board number within its BMP
    UNIQUE INDEX (bmp_id ASC, board_num ASC),
    INDEX (power_off_timestamp ASC),
    INDEX (allocated_job),
    INDEX (machine_id)
);

-- STMT
CREATE TABLE IF NOT EXISTS board_serial (
	bs_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER UNIQUE NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	bmp_serial_id CHAR(50) UNIQUE,
	physical_serial_id CHAR(50) UNIQUE
);

-- STMT
CREATE TABLE IF NOT EXISTS links (
	link_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_1 INTEGER NOT NULL,
		FOREIGN KEY (board_1)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_1 INTEGER NOT NULL,
		FOREIGN KEY (dir_1)
		REFERENCES directions(id) ON DELETE RESTRICT,
	board_2 INTEGER NOT NULL,
		FOREIGN KEY (board_2)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	dir_2 INTEGER NOT NULL,
		FOREIGN KEY (dir_2)
		REFERENCES directions(id) ON DELETE RESTRICT,
	live INTEGER NOT NULL DEFAULT (1)
		CONSTRAINT live_check CHECK (live IN (0, 1)),
	CONSTRAINT link_order_sane CHECK (board_1 <= board_2),
	UNIQUE INDEX (board_1 ASC, dir_1 ASC),
	UNIQUE INDEX (board_2 ASC, dir_2 ASC)
);

-- STMT
CREATE TABLE IF NOT EXISTS user_info (
	user_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	user_name CHAR(50) UNIQUE NOT NULL,
	encrypted_password TEXT, -- If NULL, login via OpenID
	last_successful_login_timestamp INTEGER, -- If NULL, never logged in
	-- Roles; see SecurityConfig.TrustLevel
	trust_level INTEGER NOT NULL
		CONSTRAINT trust_level_check CHECK (trust_level IN (0, 1, 2, 3)),
	-- Automatic disablement support
	failure_count INTEGER NOT NULL DEFAULT (0),
	locked INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT lock_check CHECK (locked IN (0, 1)),
	last_fail_timestamp INTEGER NOT NULL DEFAULT (0),
	-- Administrative disablement support
	disabled INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT disabled_check CHECK (disabled IN (0, 1)),
	openid_subject CHAR(100) UNIQUE,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		encrypted_password IS NOT NULL) VIRTUAL
);

-- STMT
CREATE TABLE IF NOT EXISTS user_groups (
	group_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	group_name CHAR(255) UNIQUE NOT NULL,
	quota INTEGER,				-- If NULL, no quota applies; care required, could be LARGE
	group_type INTEGER NOT NULL DEFAULT (0),
		FOREIGN KEY (group_type)
		REFERENCES group_types(id) ON DELETE RESTRICT,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		group_type = 0) VIRTUAL
);

-- STMT
CREATE TABLE IF NOT EXISTS job_states(
	id INTEGER PRIMARY KEY,
	name CHAR(50) UNIQUE NOT NULL
);

-- STMT
CREATE TABLE IF NOT EXISTS jobs (
	job_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	machine_id INTEGER,
		FOREIGN KEY (machine_id)
		REFERENCES machines(machine_id) ON DELETE RESTRICT,
	owner INTEGER NOT NULL,
		FOREIGN KEY (owner)
		REFERENCES user_info(user_id) ON DELETE RESTRICT,
	create_timestamp INTEGER NOT NULL,
	width INTEGER				-- set after allocation, in triads
		CONSTRAINT job_width_check CHECK (width > 0),
	height INTEGER				-- set after allocation, in triads
		CONSTRAINT job_height_check CHECK (height > 0),
	depth INTEGER				-- set after allocation
		CONSTRAINT job_depth_check CHECK (depth > 0),
	root_id INTEGER,				-- set after allocation
		FOREIGN KEY (root_id)
		REFERENCES boards(board_id) ON DELETE RESTRICT,
	job_state INTEGER NOT NULL,
		FOREIGN KEY (job_state)
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
		CONSTRAINT accounted_for_check CHECK (accounted_for IN (0, 1)),
	group_id INTEGER NOT NULL,
		FOREIGN KEY (group_id)
		REFERENCES user_groups(group_id) ON DELETE RESTRICT,
	-- We do not check that the user is necessarily still a member of the group;
	-- that's only a check carried out by the application on job creation. This
	-- is *by design*; users may leave groups, but their jobs do not.
	INDEX (root_id),
	INDEX (machine_id)
);

-- STMT
-- Reports of problems with boards
CREATE TABLE IF NOT EXISTS board_reports(
	report_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	job_id INTEGER,
		FOREIGN KEY (job_id)
		REFERENCES jobs(job_id),
	reported_issue TEXT NOT NULL,
	reporter INTEGER NOT NULL,
		FOREIGN KEY (reporter)
		REFERENCES user_info(user_id) ON DELETE RESTRICT,
	report_timestamp INTEGER, 	-- automatically timestamped
	UNIQUE INDEX (board_id ASC, job_id ASC, reporter ASC)
);

-- STMT
CREATE OR REPLACE VIEW jobs_usage(
	machine_id, job_id, owner, group_id, quota, size, start, finish,
	duration, quota_used, complete) AS
SELECT
	machine_id, job_id, owner, group_id, user_groups.quota, allocation_size,
	allocation_timestamp, death_timestamp,
	COALESCE(
		COALESCE(death_timestamp, UNIX_TIMESTAMP(CURRENT_TIMESTAMP)) - allocation_timestamp,
		0),
	COALESCE(allocation_size, 0) * COALESCE(
		COALESCE(death_timestamp, UNIX_TIMESTAMP(CURRENT_TIMESTAMP)) - allocation_timestamp,
		0),
	job_state = 4 -- DESTROYED
FROM jobs JOIN user_groups USING (group_id)
WHERE NOT accounted_for;

-- STMT
-- Record all allocations
CREATE TABLE IF NOT EXISTS old_board_allocations (
	alloc_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	job_id INTEGER NOT NULL,
		FOREIGN KEY (job_id)
		REFERENCES jobs(job_id) ON DELETE CASCADE,
	board_id INTEGER NOT NULL,
	    FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	alloc_timestamp INTEGER NOT NULL
);

-- STMT
DROP TRIGGER IF EXISTS alloc_tracking;

-- IGNORE
DELIMITER $$

-- STMT
CREATE TRIGGER alloc_tracking
AFTER UPDATE ON boards FOR EACH ROW
BEGIN
IF OLD.allocated_job IS NULL and NEW.allocated_job IS NOT NULL THEN
	INSERT INTO old_board_allocations(
		job_id, board_id, alloc_timestamp)
	VALUES (NEW.allocated_job, NEW.board_id,
		UNIX_TIMESTAMP(CURRENT_TIMESTAMP));
END IF;
END;
-- IGNORE
$$
DELIMITER ;

-- STMT
CREATE TABLE IF NOT EXISTS job_request (
	req_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	job_id INTEGER NOT NULL,
		FOREIGN KEY (job_id)
		REFERENCES jobs(job_id) ON DELETE CASCADE,
	num_boards INTEGER
		CONSTRAINT job_request_num_boards_sane CHECK (num_boards > 0),
	width INTEGER -- in triads
		CONSTRAINT job_request_width_sane CHECK (width > 0),
	height INTEGER -- in triads
		CONSTRAINT job_request_height_sane CHECK (height > 0),
	board_id INTEGER,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	max_dead_boards INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT job_request_max_dead_boards_sane CHECK (max_dead_boards >= 0),
	importance INTEGER NOT NULL DEFAULT (0),
	priority INTEGER NOT NULL DEFAULT (1),
	INDEX (importance DESC, job_id ASC),
	INDEX (job_id)
);

-- STMT
CREATE TABLE IF NOT EXISTS pending_changes (
    change_id INTEGER PRIMARY KEY AUTO_INCREMENT,
    job_id INTEGER,
		FOREIGN KEY (job_id)
		REFERENCES jobs(job_id) ON DELETE CASCADE,
    board_id INTEGER UNIQUE NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE RESTRICT,
    power INTEGER NOT NULL	-- Whether to switch the board on
		CONSTRAINT pending_changes_power CHECK (power IN (0, 1)),
    fpga_n INTEGER NOT NULL		-- Whether to switch the northward FPGA on
		CONSTRAINT pending_changes_fpga_n CHECK (fpga_n IN (0, 1)),
    fpga_s INTEGER NOT NULL		-- Whether to switch the southward FPGA on
		CONSTRAINT pending_changes_fpga_s CHECK (fpga_s IN (0, 1)),
    fpga_e INTEGER NOT NULL		-- Whether to switch the eastward FPGA on
		CONSTRAINT pending_changes_fpga_e CHECK (fpga_e IN (0, 1)),
    fpga_w INTEGER NOT NULL		-- Whether to switch the westward FPGA on
		CONSTRAINT pending_changes_fpga_w CHECK (fpga_w IN (0, 1)),
    fpga_nw INTEGER NOT NULL	-- Whether to switch the northwest FPGA on
		CONSTRAINT pending_changes_fpga_nw CHECK (fpga_nw IN (0, 1)),
    fpga_se INTEGER NOT NULL	-- Whether to switch the southeast FPGA on
		CONSTRAINT pending_changes_fpga_se CHECK (fpga_se IN (0, 1)),
    in_progress INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT pending_changes_in_progress CHECK (in_progress IN (0, 1)),
    from_state INTEGER NOT NULL DEFAULT (0),
		FOREIGN KEY (from_state)
		REFERENCES job_states(id) ON DELETE RESTRICT,
    to_state INTEGER NOT NULL DEFAULT (0),
		FOREIGN KEY (to_state)
		REFERENCES job_states(id) ON DELETE RESTRICT,
	INDEX (job_id)
);

-- STMT
CREATE TABLE IF NOT EXISTS blacklist_ops (
	op_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER UNIQUE NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE RESTRICT,
	op INTEGER NOT NULL			-- What we plan to do (BlacklistOperations ID)
		CONSTRAINT blacklist_ops_op CHECK (op IN (0, 1, 2)),
	data BLOB,				-- The serialized blacklist info; JAVA format!
	completed INTEGER NOT NULL DEFAULT (0)
		CONSTRAINT blacklist_ops_completed CHECK (completed IN (0, 1)),
	failure BLOB				-- The serialized exception on failure; JAVA format!
);

-- STMT
-- Coordinates of chips in a board
CREATE TABLE IF NOT EXISTS board_model_coords(
	coord_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	model INTEGER NOT NULL,
		FOREIGN KEY (model)
		REFERENCES board_models(model) ON DELETE CASCADE,
	chip_x INTEGER NOT NULL
		CONSTRAINT board_model_coords_chip_x CHECK (chip_x >= 0),
	chip_y INTEGER NOT NULL
		CONSTRAINT board_model_coords_chip_y CHECK (chip_y >= 0),
	UNIQUE INDEX (model, chip_x, chip_y)
);

-- STMT
-- Per-chip blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_chips(
	blacklist_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL,
		FOREIGN KEY (coord_id)
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	notes TEXT,
    UNIQUE INDEX (board_id ASC, coord_id ASC)
);

-- STMT
-- Per-physical-core blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_cores(
	blacklist_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL,
		FOREIGN KEY (coord_id)
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	physical_core INTEGER NOT NULL
		CONSTRAINT blacklisted_cores_physical_core CHECK (physical_core >= 0),
	notes TEXT,
	UNIQUE INDEX (board_id ASC, coord_id ASC, physical_core ASC)
);

-- STMT
-- Per-link blacklist data; may include boards not in any known machine
CREATE TABLE IF NOT EXISTS blacklisted_links(
	blacklist_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	board_id INTEGER NOT NULL,
		FOREIGN KEY (board_id)
		REFERENCES boards(board_id) ON DELETE CASCADE,
	coord_id INTEGER NOT NULL,
		FOREIGN KEY (coord_id)
		REFERENCES board_model_coords(coord_id) ON DELETE RESTRICT,
	direction INTEGER NOT NULL,
		FOREIGN KEY (direction)
		REFERENCES directions(id) ON DELETE RESTRICT,
	notes TEXT,
	UNIQUE INDEX (board_id ASC, coord_id ASC, direction ASC)
);

-- STMT
-- Many-to-many relationship model
CREATE TABLE IF NOT EXISTS group_memberships (
	membership_id INTEGER PRIMARY KEY AUTO_INCREMENT,
	user_id INTEGER NOT NULL,
		FOREIGN KEY (user_id)
		REFERENCES user_info(user_id) ON DELETE CASCADE,
	group_id INTEGER NOT NULL,
		FOREIGN KEY(group_id)
		REFERENCES user_groups(group_id) ON DELETE CASCADE,
	UNIQUE INDEX (user_id, group_id)
);

-- STMT
-- Internal users can only be in internal groups.
-- External (OIDC) users can only be in external groups.
DROP TRIGGER IF EXISTS userGroupSanity;

-- IGNORE
DELIMITER $$

-- STMT
CREATE TRIGGER userGroupSanity
AFTER INSERT ON group_memberships
FOR EACH ROW BEGIN
IF ((SELECT user_info.is_internal FROM user_info
		WHERE user_info.user_id = NEW.user_id) != (
	SELECT user_groups.is_internal FROM user_groups
		WHERE user_groups.group_id = NEW.group_id)) THEN
	SIGNAL SQLSTATE '45000' set message_text = 'group and user type don''t match';
END IF;
END;
-- IGNORE
$$
DELIMITER ;

-- STMT
-- Simulate legacy view
CREATE OR REPLACE VIEW quotas (quota_id, user_id, quota)
AS SELECT
	user_groups.group_id, user_info.user_id, user_groups.quota
FROM user_groups LEFT JOIN group_memberships USING (group_id)
LEFT JOIN user_info USING (user_id);

-- STMT
-- Supported models of SpiNNaker board
INSERT IGNORE INTO board_models(model) VALUES (2), (3), (4), (5);

-- STMT
-- The information about chip configuration of boards
INSERT IGNORE INTO board_model_coords(model, chip_x, chip_y)
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

-- STMT
-- Create boards rarely seen in the wild
INSERT IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 2, chip_x, chip_y FROM board_model_coords WHERE model = 3;

-- STMT
INSERT IGNORE INTO board_model_coords(model, chip_x, chip_y)
	SELECT 4, chip_x, chip_y FROM board_model_coords WHERE model = 5;

-- STMT
-- Standard directions between boards
INSERT IGNORE INTO directions(id, name)
VALUES
	(0, 'N'), (1, 'E'), (2, 'SE'), (3, 'S'), (4, 'W'), (5, 'NW');

-- STMT
INSERT IGNORE INTO job_states(id, name)
VALUES
	(0, 'UNKNOWN'), (1, 'QUEUED'), (2, 'POWER'), (3, 'READY'), (4, 'DESTROYED');

-- STMT
INSERT IGNORE INTO group_types(id, name)
VALUES
	(0, 'INTERNAL'), (1, 'ORGANISATION'), (2, 'COLLABRATORY');

-- STMT
INSERT IGNORE INTO movement_directions(z, direction, dx, dy, dz)
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
