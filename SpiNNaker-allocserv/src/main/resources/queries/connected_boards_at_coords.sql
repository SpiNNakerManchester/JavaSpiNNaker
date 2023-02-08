-- Copyright (c) 2021 The University of Manchester
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

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
