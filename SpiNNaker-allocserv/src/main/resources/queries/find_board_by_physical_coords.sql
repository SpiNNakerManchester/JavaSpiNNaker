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
	boards.root_x AS chip_x,
	boards.root_y AS chip_y,
	-- Coords of the root of the chip's board (same as general chip)
	boards.root_x AS board_chip_x,
	boards.root_y AS board_chip_y,
	-- Coords of the root of the job's root board (if any)
	root.root_x AS job_root_chip_x,
	root.root_y AS job_root_chip_y
FROM boards
	JOIN bmp ON boards.bmp_id = bmp.bmp_id
	JOIN machines AS m ON boards.machine_id = m.machine_id
	-- LEFT JOIN because might not be any job
	LEFT JOIN jobs ON jobs.job_id = boards.allocated_job
	LEFT JOIN boards AS root ON root.board_id = jobs.root_id
WHERE
	boards.machine_id = :machine_id
	AND bmp.cabinet = :cabinet AND bmp.frame = :frame
	AND boards.board_num = :board
LIMIT 1;
