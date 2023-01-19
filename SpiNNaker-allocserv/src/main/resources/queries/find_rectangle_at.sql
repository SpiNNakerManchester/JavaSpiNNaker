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

WITH RECURSIVE
	-- Name the arguments for sanity
	args(root_id, width, height, machine_id, max_dead_boards) AS (
		VALUES (:board_id, :width, :height, :machine_id, :max_dead_boards)),
	-- Profile the machines and boards to the one we care about
	m AS (
		SELECT machines.* FROM machines JOIN args USING (machine_id)
		LIMIT 1),
	bs AS (
		SELECT boards.* FROM boards JOIN args USING (machine_id)),
	selected_root AS (
		SELECT bs.* FROM bs JOIN args ON bs.board_id = args.root_id
		LIMIT 1),
	-- Generate sequences of right size
	cx(x) AS (SELECT 0 UNION ALL SELECT x+1 FROM cx
		LIMIT (SELECT width FROM args)),
	cy(y) AS (SELECT 0 UNION ALL SELECT y+1 FROM cy
		LIMIT (SELECT height FROM args)),
	cz(z) AS (VALUES (0), (1), (2)),
	-- Form the sequences into grids of points
	c(x,y,z) AS (SELECT x, y, z FROM cx, cy, cz),
	-- Count boards in rectangle based at specified root
	root_count(available) AS (
		SELECT SUM(bs.may_be_allocated)
		FROM selected_root, m, c, bs
		WHERE bs.x = (c.x + selected_root.x) % m.width
			AND bs.y = (c.y + selected_root.y) % m.height
			AND bs.z = c.z)
SELECT
	selected_root.board_id AS id,
	selected_root.x AS x,
	selected_root.y AS y,
	selected_root.z AS z,
	root_count.available AS available
FROM args, selected_root, root_count
WHERE root_count.available >= args.width * args.height - args.max_dead_boards
	AND selected_root.may_be_allocated > 0
LIMIT 1;
