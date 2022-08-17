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
	-- Name the arguments for sanity
	args(width, height, machine_id, max_dead_boards) AS (
		VALUES (:width, :height, :machine_id, :max_dead_boards)),
	-- Profile the machines and boards to the one we care about
	m AS (SELECT machines.* FROM machines JOIN args USING (machine_id) LIMIT 1),
	bs AS (SELECT boards.* FROM boards JOIN args USING (machine_id))
SELECT
	root.board_id AS id,
	root.x AS x, root.y AS y, root.z AS z,
	root.available AS available
FROM args, bs, (
	WITH RECURSIVE
		-- Generate sequences of right size
		cx(x) AS (SELECT 0 UNION SELECT x+1 FROM cx, args
			WHERE x < args.width - 1),
		cy(y) AS (SELECT 0 UNION SELECT y+1 FROM cy, args
			WHERE y < args.height - 1),
		triad(z) AS (VALUES (0), (1), (2)),
		gx(x) AS (SELECT 0 UNION SELECT x+1 FROM gx, m WHERE x < m.width - 1),
		gy(y) AS (SELECT 0 UNION SELECT y+1 FROM gy, m WHERE y < m.height - 1),
		-- Form the sequences into grids of points
		c(x,y,z) AS (SELECT x, y, z FROM cx, cy, triad),
		g(x,y) AS (SELECT x, y FROM gx, gy)
	SELECT board_id, bs.x AS x, bs.y AS y, bs.z AS z,
		SUM(bs.may_be_allocated) AS available
	FROM bs, c, g, args, m
	WHERE bs.x = (c.x + g.x) % m.width AND bs.y = (c.y + g.y) % m.height
		AND bs.z = c.z
	GROUP BY g.x, g.y) AS root
WHERE available >= args.width * args.height - args.max_dead_boards
	AND bs.board_id = id AND bs.may_be_allocated > 0
ORDER BY z ASC, y ASC, x ASC;
