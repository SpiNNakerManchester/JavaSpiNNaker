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
	args(job, root, x, y) AS (VALUES (:job_id, :board_id, :x, :y)),
	-- Boards that are allocated to the job
	bs AS (
		SELECT boards.* FROM boards, args
		WHERE boards.allocated_job = args.job),
	-- The root board of the job
	root AS (
		SELECT bs.* FROM bs, args WHERE bs.board_id = args.root
		LIMIT 1),
	-- The machine of the job
	m AS (
		SELECT machines.* FROM machines JOIN root USING (machine_id)
		LIMIT 1),
	wrapped(x, y) AS (
		SELECT
			CASE WHEN args.x + root.root_x <= m.max_chip_x
				THEN args.x + root.root_x
				ELSE args.x + root.root_x - m.max_chip_x - 1
				END AS x,
			CASE WHEN args.y + root.root_y <= m.max_chip_y
				THEN args.y + root.root_y
				ELSE args.y + root.root_y - m.max_chip_y - 1
				END AS y
		FROM m, args, root)
SELECT
	bs.board_id, bs.address, bs.x, bs.y, bs.z,
	bs.allocated_job AS job_id, m.machine_name,
	bmp.cabinet, bmp.frame, bs.board_num,
	args.x AS chip_x, args.y AS chip_y,
	CASE WHEN bs.root_x - root.root_x >= 0
		THEN bs.root_x - root.root_x
		ELSE bs.root_x - root.root_x + m.max_chip_x + 1
		END AS board_chip_x,
	CASE WHEN bs.root_y - root.root_y >= 0
		THEN bs.root_y - root.root_y
		ELSE bs.root_y - root.root_y + m.max_chip_y + 1
		END AS board_chip_y,
	root.root_x AS job_root_chip_x,
	root.root_y AS job_root_chip_y
FROM bs
	JOIN bmp USING (bmp_id)
	JOIN m
	JOIN board_model_coords AS bmc ON m.board_model = bmc.model
	JOIN wrapped
	JOIN root
	JOIN args
WHERE wrapped.x = bs.root_x + bmc.chip_x
	AND wrapped.y = bs.root_y + bmc.chip_y
LIMIT 1;
