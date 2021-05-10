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
	args(width, height, machine_id, max_dead_boards) AS (VALUES (?, ?, ?, ?)),
	-- Profile the machines and boards to the one we care about
	m AS (SELECT machines.* FROM machines
		JOIN args ON machines.machine_id = args.machine_id LIMIT 1),
	bs AS (SELECT boards.* FROM boards
		JOIN args ON boards.machine_id = args.machine_id)
SELECT board_id AS id, x, y, available FROM args, (
	WITH RECURSIVE
		-- Generate sequences of right size
		cx(x) AS (SELECT 0 UNION SELECT x+1 FROM cx, args
			WHERE x < args.width - 1),
		cy(y) AS (SELECT 0 UNION SELECT y+1 FROM cy, args
			WHERE y < args.height - 1),
		gx(x) AS (SELECT 0 UNION SELECT x+1 FROM gx, m WHERE x < m.width - 1),
		gy(y) AS (SELECT 0 UNION SELECT y+1 FROM gy, m WHERE y < m.height - 1),
		-- Form the sequences into grids of points
		c(x,y) AS (SELECT x, y FROM cx, cy),
		g(x,y) AS (SELECT x, y FROM gx, gy)
	SELECT board_id, bs.root_x AS x, bs.root_y AS y,
		SUM(bs.may_be_allocated) AS available
	FROM bs, c, g, args
	WHERE bs.root_x=c.x+g.x AND bs.root_y=c.y+g.y
	GROUP BY g.x, g.y)
WHERE available >= args.width * args.height - args.max_dead_boards
ORDER BY y ASC, x ASC;
