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
	args(machine_id, x, y, width, height) AS (
		VALUES (:machine_id, :x, :y, :width, :height)),
	xrange(n) AS (
		SELECT 0 UNION ALL SELECT n+1 FROM xrange
		LIMIT (SELECT width FROM args)),
	yrange(n) AS (
		SELECT 0 UNION ALL SELECT n+1 FROM yrange
		LIMIT (SELECT height FROM args)),
	-- The logical rectangle of interest
	rect(x, y) AS (
		SELECT
			(args.x + xrange.n) % machines.width,
			(args.y + yrange.n) % machines.height
		FROM args, xrange, yrange
		JOIN machines USING (machine_id)
		ORDER BY x, y),
	-- Boards on the machine in the rectangle of interest that are allocatable
	bs AS (SELECT board_id FROM boards
		JOIN args USING (machine_id)
		-- This query ignores Z; uses whole triads always
		JOIN rect USING (x, y)
		WHERE may_be_allocated
		ORDER BY board_id),
	-- Links between boards of interest that are live
	ls AS (SELECT board_1, board_2 FROM links
		JOIN bs AS b1 ON links.board_1 = b1.board_id
		JOIN bs AS b2 ON links.board_2 = b2.board_id
		WHERE live
		ORDER BY board_1, board_2),
	-- Follow the connectivity graph; SQLite magic!
	connected(b) AS (
		SELECT board_id FROM boards JOIN args USING (x, y)
			WHERE boards.z = 0 AND may_be_allocated
		UNION
		SELECT ls.board_2 FROM connected JOIN ls ON board_1 == b
		UNION
		SELECT ls.board_1 FROM connected JOIN ls ON board_2 == b
		ORDER BY board_id)
SELECT
	count(*) AS connected_size
FROM connected;
