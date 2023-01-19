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
	-- Arguments and current timestamp
	args(machine_id, now) AS (
		SELECT :machine_id, :time_now),
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
