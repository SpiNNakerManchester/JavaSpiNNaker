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

-- --------------------------------------------------------------------------
-- Get jobs on a machine that have changes that can be processed. This
-- respects a machine-level policy on how long a board must be switched off
-- before it can be switched on again, and how long a board must be switched
-- on before it can be switched off.

WITH
	-- Arguments and current timestamp
	args(machine_id, now) AS (
		SELECT :machine_id, UNIX_TIMESTAMP()),
	-- The machine on which we are allocating
	m AS (SELECT machines.* FROM machines JOIN args USING (machine_id) LIMIT 1),
	-- The set of boards that are "busy" settling after changing state
	busy_boards AS (
		SELECT boards.board_id FROM boards, args, m
		WHERE boards.power_on_timestamp + m.on_delay > args.now
			OR boards.power_off_timestamp + m.off_delay > args.now)
SELECT
	-- The IDs of jobs
	jobs.job_id
FROM jobs JOIN args USING (machine_id)
	-- the jobs are on the machine of interest
WHERE
	-- and the jobs have some pending changes
	EXISTS(
		SELECT 1 from pending_changes
		WHERE pending_changes.job_id = jobs.job_id)
	-- and the jobs do not have pending changes on the busy boards
	AND NOT EXISTS(
		SELECT 1 FROM
			pending_changes JOIN busy_boards USING (board_id)
		WHERE pending_changes.job_id = jobs.job_id);
