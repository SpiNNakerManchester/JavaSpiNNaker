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
INSERT IGNORE INTO tombstone.jobs(
	job_id, machine_id, owner, create_timestamp,
	width, height, "depth", root_id,
	keepalive_interval, keepalive_host,
	death_reason, death_timestamp,
	original_request,
	allocation_timestamp, allocation_size,
	machine_name, owner_name, "group", group_name)
SELECT
	jobs.job_id, jobs.machine_id, jobs.owner, jobs.create_timestamp,
	jobs.width, jobs.height, jobs."depth", jobs.allocated_root,
	jobs.keepalive_interval, jobs.keepalive_host,
	jobs.death_reason, jobs.death_timestamp,
	original_request,
	allocation_timestamp, allocation_size,
	machines.machine_name, user_info.user_name,
	groups.group_id, groups.group_name
FROM
	jobs
	JOIN groups USING (group_id)
	JOIN machines USING (machine_id)
	JOIN user_info ON jobs.owner = user_info.user_id
WHERE death_timestamp + :grace_period < :time_now
RETURNING job_id;
