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
-- Get the links on the perimeter of the allocation to a job. The perimeter
-- is defined as being the live links between a board that is part of the
-- allocation and a board that is not.

WITH bs AS (
	-- Boards that are allocated to the job
	SELECT board_id FROM boards WHERE allocated_job = :job_id)
SELECT board_1 AS board_id, dir_1 AS direction FROM links
	WHERE board_1 IN (SELECT board_id FROM bs)
	AND live
	AND NOT board_2 IN (SELECT board_id FROM bs)
UNION
SELECT board_2 AS board_id, dir_2 AS direction FROM links
	WHERE board_2 IN (SELECT board_id FROM bs)
	AND live
	AND NOT board_1 IN (SELECT board_id FROM bs);
