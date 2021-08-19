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

WITH
	t(now) AS (VALUES (CAST(strftime('%s','now') AS INTEGER)))
INSERT OR IGNORE INTO tombstone.jobs(
	job_id, machine_id, owner, create_timestamp,
	width, height, "depth", root_id,
	keepalive_interval, keepalive_host,
	death_reason, death_timestamp,
	original_request,
	allocation_timestamp, allocation_size,
	machine_name, owner_name)
SELECT
	jobs.job_id, jobs.machine_id, jobs.owner, jobs.create_timestamp,
	jobs.width, jobs.height, jobs."depth", jobs.allocated_root,
	jobs.keepalive_interval, jobs.keepalive_host,
	jobs.death_reason, jobs.death_timestamp,
	original_request,
	allocation_timestamp, allocation_size,
	machines.machine_name, user_info.user_name
FROM
	jobs
	JOIN machines USING (machine_id)
	JOIN user_info ON jobs.owner = user_info.user_id
	JOIN t
WHERE death_timestamp + :grace_period < t.now
RETURNING job_id;
