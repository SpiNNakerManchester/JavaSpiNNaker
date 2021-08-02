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
	args(machine_id, x, y, width, height) AS (
		VALUES (:machine_id, :x, :y, :width, :height)),
	m AS (SELECT machines.* FROM machines
		JOIN args ON machines.machine_id = args.machine_id),
	-- Boards on the machine in the rectangle of interest
	bs AS (SELECT boards.* FROM boards
		JOIN args ON boards.machine_id = args.machine_id
		JOIN m
		WHERE boards.x >= args.x
			AND boards.x < (args.x + args.width) % m.width
			AND boards.y >= args.y
			AND boards.y < (args.y + args.height) % m.height
			AND may_be_allocated),
	-- Links between boards of interest
	ls AS (SELECT links.* FROM links
		WHERE links.board_1 IN (SELECT board_id FROM bs)
			AND links.board_2 IN (SELECT board_id FROM bs)
			AND links.live),
	-- Follow the connectivity graph; SQLite magic!
	connected(b) AS (
		SELECT board_id FROM bs,args
			WHERE bs.x = args.x AND bs.y = args.y AND bs.z = 0
		UNION
		SELECT ls.board_2 FROM connected JOIN ls ON board_1 == b
		UNION
		SELECT ls.board_1 FROM connected JOIN ls ON board_2 == b)
SELECT
	count(b) AS connected_size
FROM connected;
