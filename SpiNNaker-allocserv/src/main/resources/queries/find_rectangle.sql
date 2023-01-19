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
	-- Name the arguments for sanity
	args(width, height, machine_id, max_dead_boards) AS (
		SELECT :width, :height, :machine_id, :max_dead_boards),
	-- Profile the machines and boards to the one we care about
	m AS (SELECT machines.* FROM machines JOIN args USING (machine_id) LIMIT 1),
	-- Generate sequences of right size
	cx(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM cx, args
		WHERE x < args.width - 1),
	cy(y) AS (SELECT 0 UNION ALL SELECT y+1 FROM cy, args
		WHERE y < args.height - 1),
	triad(z) AS (SELECT 0 UNION SELECT 1 UNION SELECT 2),
	gx(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM gx, m
		WHERE x < m.width - 1),
	gy(y) AS (SELECT 0 UNION ALL SELECT y+1 FROM gy, m
		WHERE y < m.height - 1),
	-- Form the sequences into grids of points
	c(x,y,z) AS (SELECT x, y, z FROM cx, cy, triad),
	g(x,y) AS (SELECT x, y FROM gx, gy)
SELECT
	DISTINCT(boards.board_id) AS id,
	boards.x, boards.y, boards.z,
	root.available AS available, boards.power_off_timestamp
FROM args, boards, (
	SELECT min(board_id) as board_id,
		SUM(boards.may_be_allocated) AS available
	FROM args JOIN c JOIN g JOIN boards USING (machine_id) JOIN m
	WHERE
		boards.x = (c.x + g.x) % m.width AND boards.y = (c.y + g.y) % m.height
		AND boards.z = c.z
	GROUP BY g.x, g.y) AS root
WHERE available >= args.width * args.height - args.max_dead_boards
	AND boards.board_id = root.board_id AND boards.may_be_allocated > 0
ORDER BY power_off_timestamp ASC;
