-- Copyright (c) 2021-2023 The University of Manchester
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

-- Get current allocation tasks
SELECT
	job_request.req_id,
	job_request.job_id,
	job_request.num_boards,
	job_request.width,
	job_request.height,
	job_request.board_id,
	jobs.machine_id AS machine_id,
	job_request.max_dead_boards,
	machines.width AS max_width,
	machines.height AS max_height,
	jobs.job_state AS job_state,
	job_request.importance
FROM
	job_request
	JOIN jobs USING (job_id)
	JOIN machines USING (machine_id)
WHERE job_state = :job_state
ORDER BY importance DESC, req_id ASC;
