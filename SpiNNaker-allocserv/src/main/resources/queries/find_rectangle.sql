-- Copyright (c) 2021 The University of Manchester
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     https://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

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
	c(x,y,z) AS (SELECT x, y, z FROM cx, cy, triad ORDER BY x, y, z),
	g(x,y) AS (SELECT x, y FROM gx, gy ORDER BY x, y),
    -- Root coords and number of boards available from that point
	-- NB: Can't use board ID safely as we are using a GROUP BY
	root(x,y,z,available) AS (
		SELECT g.x AS x, g.y AS y, 0 AS z,
			SUM(boards.may_be_allocated) AS available
		FROM args
			JOIN c
			JOIN g
			JOIN boards USING (machine_id)
			JOIN m
		WHERE
			boards.x = (c.x + g.x) % m.width
			AND boards.y = (c.y + g.y) % m.height
			AND boards.z = c.z
		GROUP BY g.x, g.y)
SELECT
	boards.board_id AS id,
	boards.x AS x,
	boards.y AS y,
	boards.z AS z,
	root.available AS available
FROM args
	JOIN boards USING (machine_id)
	JOIN root USING (x, y, z)
WHERE root.available >= args.width * args.height - args.max_dead_boards
	AND boards.may_be_allocated > 0
ORDER BY power_off_timestamp ASC;
