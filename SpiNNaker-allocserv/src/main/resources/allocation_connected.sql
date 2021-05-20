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
	args(machine_id, x, y, width, height) AS (VALUES (?, ?, ?, ?, ?)),
	bs AS (SELECT boards.* FROM boards
		JOIN args ON boards.machine_id = args.machine_id
		WHERE boards.x >= args.x AND boards.x < args.x + args.width
			AND boards.y >= args.y AND boards.y < args.y + args.height
			AND may_be_allocated > 0),
	ls AS (SELECT links.* FROM links
		WHERE links.board_1 IN (SELECT board_id FROM bs)
			AND links.board_2 IN (SELECT board_id FROM bs)
			AND links.live),
	connected(b) AS (
		SELECT board_id FROM bs,args WHERE bs.x=args.x AND bs.y=args.y
		UNION
		SELECT ls.board_2 FROM connected JOIN ls ON board_1 == b
		UNION
		SELECT ls.board_1 FROM connected JOIN ls ON board_2 == b)
SELECT
	count(b) AS connected_size
FROM connected;
