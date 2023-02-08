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

-- --------------------------------------------------------------------------
-- Locate a board (using a full set of coordinates) based on global chip
-- coordinates.

SELECT
	-- IDs
	boards.board_id,
	boards.bmp_id,
	boards.allocated_job AS job_id,
	-- General info
	m.machine_name,
	boards.address,
	-- Triad (logical) coords
	boards.x,
	boards.y,
	boards.z,
	-- Cabinet/frame/board (physical) coords
	bmp.cabinet,
	bmp.frame,
	boards.board_num,
	-- Coords of the chip
	boards.root_x + bmc.chip_x AS chip_x,
	boards.root_y + bmc.chip_y AS chip_y,
	-- Coords of the root of the chip's board
	boards.root_x AS board_chip_x,
	boards.root_y AS board_chip_y,
	-- Coords of the root of the job's root board (if any)
	root.root_x AS job_root_chip_x,
	root.root_y AS job_root_chip_y
FROM boards
	JOIN bmp USING (bmp_id)
	JOIN machines AS m USING (machine_id)
	JOIN board_model_coords AS bmc ON m.board_model = bmc.model
	-- LEFT JOIN because might not be any job
	LEFT JOIN jobs ON jobs.job_id = boards.allocated_job
	LEFT JOIN boards AS root ON root.board_id = jobs.root_id
WHERE
	boards.machine_id = :machine_id AND chip_x = :x AND chip_y = :y
LIMIT 1;
