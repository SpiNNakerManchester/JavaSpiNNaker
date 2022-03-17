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
INSERT OR IGNORE INTO tombstone.board_allocations(
	alloc_id, job_id, board_id, allocation_timestamp)
SELECT
	src.alloc_id, src.job_id, src.board_id, src.alloc_timestamp
FROM
	old_board_allocations AS src
	JOIN jobs USING (job_id)
	JOIN t
WHERE jobs.death_timestamp + :grace_period < t.now
RETURNING alloc_id;
