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

WITH RECURSIVE
	args(machine_id, x, y, z, width, height, "depth") AS (
		VALUES (:machine_id, :x, :y, :z, :width, :height, :depth)),
	m AS (SELECT machines.* FROM machines
		JOIN args USING (machine_id)),
	-- The logical rectangle of interest
	rect(x, y, z, local_x, local_y, local_z) AS (
		SELECT x, y, z, 0, 0, 0 FROM args
		UNION
		SELECT (rect.x + 1) % m.width, rect.y, rect.z, local_x + 1, local_y, local_z
			FROM rect, m, args
			WHERE local_x + 1 < args.width
		UNION
		SELECT rect.x, (rect.y + 1) % m.height, rect.z, local_x, local_y + 1, local_z
			FROM rect, m, args
			WHERE local_y + 1 < args.height
		UNION
		SELECT rect.x, rect.y, (rect.z + 1) % m."depth", local_x, local_y, local_z + 1
			FROM rect, m, args
			WHERE local_z + 1 < args."depth"),
	-- Boards on the machine in the rectangle of interest
	bs AS (SELECT boards.* FROM boards
		JOIN m USING (machine_id)
		JOIN rect USING (x, y, z)
		WHERE may_be_allocated),
	-- Links between boards of interest
	ls AS (SELECT links.* FROM links
		WHERE links.board_1 IN (SELECT board_id FROM bs)
			AND links.board_2 IN (SELECT board_id FROM bs)
			AND links.live),
	-- Follow the connectivity graph; SQLite magic!
	connected(b) AS (
		SELECT board_id FROM bs JOIN args USING (x, y, z)
		UNION
		SELECT ls.board_2 FROM connected JOIN ls ON board_1 == b
		UNION
		SELECT ls.board_1 FROM connected JOIN ls ON board_2 == b)
SELECT
	bs.board_id
FROM bs JOIN connected ON bs.board_id = connected.b
ORDER BY bs.x ASC, bs.y ASC, bs.z ASC;
