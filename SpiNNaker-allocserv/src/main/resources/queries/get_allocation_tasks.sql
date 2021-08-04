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

-- Get current allocation tasks
SELECT
	job_request.req_id,
	job_request.job_id,
	job_request.num_boards,
	job_request.width,
	job_request.height,
	job_request.x,
	job_request.y,
	job_request.z,
	jobs.machine_id AS machine_id,
	job_request.max_dead_boards,
	machines.width AS max_width,
	machines.height AS max_height,
	jobs.job_state AS job_state
FROM
	job_request
	JOIN jobs ON job_request.job_id = jobs.job_id
	JOIN machines ON jobs.machine_id = machines.machine_id
ORDER BY req_id;
