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

-- --------------------------------------------------------------------------
-- Get the set of boards at some coordinates within a triad rectangle that
-- are connected (i.e., have at least one path over enableable links within
-- the allocation) to the root board.

WITH RECURSIVE
	args(machine_id, x, y, z, width, height, "depth") AS (
		VALUES (:machine_id, :x, :y, :z, :width, :height, :depth)),
	-- The logical rectangle of interest
	rect(x, y, z) AS MATERIALIZED (
		WITH
			m(w, h, d) AS (
				SELECT machines.width, machines.height, machines."depth"
				FROM machines JOIN args USING (machine_id)
				LIMIT 1),
			xrange(n) AS (
				SELECT 0 UNION ALL SELECT n+1 FROM xrange
				LIMIT (SELECT width FROM args)),
			yrange(n) AS (
				SELECT 0 UNION ALL SELECT n+1 FROM yrange
				LIMIT (SELECT height FROM args)),
			zrange(n) AS (
				SELECT 0 UNION ALL SELECT n+1 FROM zrange
				LIMIT (SELECT "depth" FROM args))
		SELECT (args.x + xrange.n) % m.w, (args.y + yrange.n) % m.h,
			(args.z + zrange.n) % m.d
		FROM args, xrange, yrange, zrange, m
		ORDER BY x, y, z),
	-- Boards on the machine in the rectangle of interest
	bs(board_id, x, y, z) AS (
		SELECT boards.board_id, boards.x, boards.y, boards.z FROM boards
		JOIN args USING (machine_id)
		JOIN rect USING (x, y, z)
		WHERE may_be_allocated
		ORDER BY 2, 3, 4),
	-- Links between boards of interest
	ls(b1, b2) AS (SELECT board_1, board_2 FROM links
		WHERE board_1 IN (SELECT board_id FROM bs)
			AND board_2 IN (SELECT board_id FROM bs)
			AND live
		ORDER BY 1, 2),
	-- Follow the connectivity graph; SQLite magic!
	connected(b) AS (
		SELECT board_id FROM boards JOIN args USING (machine_id, x, y, z)
			WHERE may_be_allocated
		UNION
		SELECT ls.b2 FROM connected JOIN ls ON b1 == b
		UNION
		SELECT ls.b1 FROM connected JOIN ls ON b2 == b
		ORDER BY 1)
SELECT
	bs.board_id
FROM bs JOIN connected ON bs.board_id = connected.b
ORDER BY bs.x ASC, bs.y ASC, bs.z ASC;
