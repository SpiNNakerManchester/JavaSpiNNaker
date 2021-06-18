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
	args(machine_id, cabinet, frame, board) AS (
		VALUES (:machine_id, :cabinet, :frame, :board))
SELECT
	x, y, z
FROM args
	JOIN boards
	JOIN bmp
	ON boards.bmp_id = bmp.bmp_id
WHERE boards.machine_id = args.machine_id
	AND boards.board_num = args.board
	AND bmp.cabinet = args.cabinet
	AND bmp.frame = args.frame
	AND boards.may_be_allocated > 0;
