-- Copyright (c) 2022 The University of Manchester
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
	openid_subject, NULL
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
