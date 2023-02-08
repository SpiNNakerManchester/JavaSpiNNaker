-- Copyright (c) 2022 The University of Manchester
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

-- New table definition
CREATE TABLE new_user_info (
	user_id INTEGER PRIMARY KEY AUTOINCREMENT,
	user_name TEXT UNIQUE NOT NULL,
	encrypted_password TEXT, -- If NULL, login via OpenID
	last_successful_login_timestamp INTEGER, -- If NULL, never logged in
	-- Roles; see SecurityConfig.TrustLevel
	trust_level INTEGER NOT NULL CHECK (trust_level IN (0, 1, 2, 3)),
	-- Automatic disablement support
	failure_count INTEGER NOT NULL DEFAULT (0),
	locked INTEGER NOT NULL DEFAULT (0) CHECK (locked IN (0, 1)),
	last_fail_timestamp INTEGER NOT NULL DEFAULT (0),
	-- Administrative disablement support
	disabled INTEGER NOT NULL DEFAULT (0) CHECK (disabled IN (0, 1)),
	openid_subject TEXT UNIQUE,
	is_internal INTEGER GENERATED ALWAYS AS ( -- generated COLUMN
		encrypted_password IS NOT NULL) VIRTUAL
);

-- Copy data
INSERT INTO new_user_info(user_id, user_name, encrypted_password,
	last_successful_login_timestamp, trust_level, failure_count,
	locked, last_fail_timestamp, disabled,
	openid_subject)
SELECT
	user_id, user_name, encrypted_password,
	last_successful_login_timestamp, trust_level, failure_count,
	locked, last_fail_timestamp, disabled,
	NULL
FROM user_info;

-- Drop dependencies (will be auto-rebuilt on app boot)
DROP VIEW quotas;
DROP TRIGGER userGroupSanity;

-- Move the new table definition into place
DROP TABLE user_info;
ALTER TABLE new_user_info RENAME TO user_info;


PRAGMA integrity_check;
PRAGMA foreign_key_check;
PRAGMA user_version=2;
COMMIT;
PRAGMA foreign_keys=ON;
