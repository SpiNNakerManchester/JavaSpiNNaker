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
	args(job, root, x, y) AS (VALUES (?, ?, ?, ?)),
	-- Boards that are allocated to the job
	bs AS (
		SELECT boards.* FROM boards, args
		WHERE boards.allocated_job = args.job),
	-- The root board of the job
	root AS (SELECT bs.* FROM bs, args WHERE boards.board_id = args.root)
SELECT
	bs.board_id, address, x, y, z,
	job_id, m.machine_name, bmp.cabinet, bmp.frame,
	bs.board_num, args.x AS chip_x, args.y AS chip_y
FROM bs
	JOIN bmp ON bs.bmp_id = bmp.bmp_id
	JOIN machines AS m ON bs.machine_id = m.machine_id
	JOIN board_model_coords AS bmc
		ON m.board_model = bmc.model
	JOIN args
WHERE args.x + root.root_x = bs.root_x + bmc.chip_x
	AND args.y + root.root_y = bs.root_y + bmc.chip_y
LIMIT 1;
