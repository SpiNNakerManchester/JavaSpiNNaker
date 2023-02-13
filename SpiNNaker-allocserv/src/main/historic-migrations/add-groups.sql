-- Copyright (c) 2022-2023 The University of Manchester
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

-- This is for spalloc.sqlite3

-- See https://www.sqlite.org/lang_altertable.html#otheralter
PRAGMA foreign_keys=OFF;
BEGIN;
-- First, check we're operating on the right database!
SELECT count(machine_id) FROM machines;

CREATE TABLE group_types (
	"id" INTEGER PRIMARY KEY,
	"name" TEXT UNIQUE NOT NULL
);
INSERT INTO group_types("id", name)
VALUES
	(0, 'INTERNAL'), (1, 'ORGANISATION'), (2, 'COLLABRATORY');
-- Lock down the group_types
CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_update"
BEFORE UPDATE ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_insert"
BEFORE INSERT ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TRIGGER IF NOT EXISTS "group_types_is_static no_delete"
BEFORE DELETE ON group_types
BEGIN
	SELECT RAISE(IGNORE);
END;

CREATE TABLE groups (
	group_id INTEGER PRIMARY KEY AUTOINCREMENT,
	group_name TEXT UNIQUE NOT NULL,
	quota INTEGER, -- If NULL, no quota applies; care required, could be LARGE
	group_type INTEGER NOT NULL DEFAULT (0) REFERENCES group_types(id) ON DELETE RESTRICT,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		group_type = 0) VIRTUAL
);
INSERT INTO groups(group_name, quota, group_type)
VALUES
	('!! LEGACY-1 !!', NULL, 0), ('!! LEGACY-2 !!', 0, 1);

ALTER TABLE user_info ADD COLUMN
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		encrypted_password IS NOT NULL) VIRTUAL;

CREATE TABLE group_memberships (
	membership_id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_id INTEGER NOT NULL REFERENCES user_info(user_id) ON DELETE CASCADE,
	group_id INTEGER NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE
);
-- No user can be in a group more than once!
CREATE UNIQUE INDEX membershipSanity ON group_memberships(
	user_id, group_id
);
-- Internal users can only be in internal groups.
-- External (OIDC) users can only be in external groups.
CREATE TRIGGER userGroupSanity
AFTER INSERT ON group_memberships
WHEN
	(SELECT user_info.is_internal FROM user_info
		WHERE user_info.user_id = NEW.user_id) != (
	SELECT groups.is_internal FROM groups
		WHERE groups.group_id = NEW.group_id)
BEGIN
	SELECT RAISE(FAIL, 'group and user type don''t match');
END;

CREATE TABLE new_jobs (
	job_id INTEGER PRIMARY KEY AUTOINCREMENT,
	machine_id INTEGER REFERENCES machines(machine_id) ON DELETE RESTRICT,
	owner INTEGER NOT NULL REFERENCES user_info(user_id) ON DELETE RESTRICT,
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
	num_pending INTEGER NOT NULL DEFAULT (0),
	allocation_timestamp INTEGER, -- timestamp
	allocation_size INTEGER,
	allocated_root INTEGER, -- set by trigger
	accounted_for INTEGER NOT NULL DEFAULT (0) CHECK (accounted_for IN (0, 1)),
	group_id INTEGER NOT NULL REFERENCES groups(group_id) ON DELETE RESTRICT
);
INSERT INTO new_jobs(
	job_id, machine_id, owner, create_timestamp, width, height, "depth",
	root_id, job_state, keepalive_interval, keepalive_timestamp, keepalive_host,
	death_reason, death_timestamp, original_request,
	num_pending, allocation_timestamp, allocation_size, allocated_root,
	accounted_for, group_id)
SELECT
	jobs.job_id, jobs.machine_id, jobs.owner, jobs.create_timestamp,
	jobs.width, jobs.height, jobs."depth", jobs.root_id, jobs.job_state,
	jobs.keepalive_interval, jobs.keepalive_timestamp, jobs.keepalive_host,
	jobs.death_reason, jobs.death_timestamp, jobs.original_request,
	jobs.num_pending, jobs.allocation_timestamp, jobs.allocation_size,
	jobs.allocated_root, jobs.accounted_for,
	CASE
		WHEN user_info.is_internal THEN 1 -- = LEGACY-1
		ELSE 2 -- = LEGACY-2
	END AS group_id
FROM jobs JOIN user_info ON jobs.owner = user_info.user_id;
DROP VIEW jobs_usage;
DROP TABLE jobs;
DROP TRIGGER boardDeallocation;
ALTER TABLE new_jobs RENAME TO jobs;
--==-- IMPORTANT! DON'T REBUILD THE TRIGGERS HERE! --==--
CREATE INDEX IF NOT EXISTS 'jobs_root_id' ON 'jobs'('root_id'); --> boards(board_id)
CREATE INDEX IF NOT EXISTS 'jobs_machine_id' ON 'jobs'('machine_id'); --> machines(machine_id)

DROP TABLE quotas;
CREATE VIEW quotas (quota_id, user_id, machine_id, quota)
AS SELECT
	groups.group_id, user_info.user_id, machines.machine_id, groups.quota
FROM groups
	LEFT JOIN group_memberships USING (group_id)
	LEFT JOIN user_info USING (user_id)
	LEFT JOIN machines;


CREATE VIEW jobs_usage(
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

PRAGMA integrity_check;
PRAGMA foreign_key_check;
PRAGMA user_version=2;
COMMIT;
PRAGMA foreign_keys=ON;
