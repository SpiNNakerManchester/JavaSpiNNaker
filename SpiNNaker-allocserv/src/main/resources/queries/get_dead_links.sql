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

-- Get the dead links of a machine; each link is described by the boards it
-- links and the directions the link goes in at each end.
SELECT
	-- Link end 1: board coords + direction
	b1.x AS board_1_x,
	b1.y AS board_1_y,
	b1.z AS board_1_z,
	bmp1.cabinet AS board_1_c,
	bmp1.frame AS board_1_f,
	b1.board_num AS board_1_b,
	b1.address AS board_1_addr,
	dir_1,
	-- Link end 2: board coords + direction
	b2.x AS board_2_x,
	b2.y AS board_2_y,
	b2.z AS board_2_z,
	bmp2.cabinet AS board_2_c,
	bmp2.frame AS board_2_f,
	b2.board_num AS board_2_b,
	b2.address AS board_2_addr,
	dir_2
FROM links
	JOIN boards AS b1 ON board_1 = b1.board_id
	JOIN boards AS b2 ON board_2 = b2.board_id
	JOIN bmp AS bmp1 ON bmp1.bmp_id = b1.bmp_id
	JOIN bmp AS bmp2 ON bmp2.bmp_id = b2.bmp_id
WHERE
	b1.machine_id = :machine_id AND NOT live
ORDER BY board_1 ASC, board_2 ASC;
