-- Copyright (c) 2021 The University of Manchester
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
