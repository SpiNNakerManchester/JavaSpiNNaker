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
	args(machine, on_delay, off_delay, now) AS (
		VALUES (?, ?, ?, strftime('%s', 'now'))),
	-- The set of boards that are "busy" settling after changing state
	busy_boards AS (
		SELECT boards.board_id FROM boards, args
		WHERE boards.power_on_timestamp + args.on_delay > args.now
			OR boards.power_off_timestamp + args.off_delay > args.now)
SELECT
	-- The IDs of jobs
	jobs.job_id
FROM jobs JOIN args
WHERE
	-- the jobs ae on the machine of interest
	jobs.machine_id == args.machine
	-- and the jobs have some pending changes
	AND EXISTS(
		SELECT 1 from pending_changes
		WHERE pending_changes.job_id = jobs.job_id)
	-- and the jobs do not have pending changes on the busy boards
	AND NOT EXISTS(
		SELECT 1 FROM
			pending_changes JOIN busy_boards
			ON pending_changes.board_id = busy_boards.board_id
		WHERE pending_changes.job_id = jobs.job_id);
