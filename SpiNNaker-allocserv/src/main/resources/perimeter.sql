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
	-- Boards that are allocated to the job
	bs AS (SELECT boards.* FROM boards WHERE boards.allocated_job = ?)
SELECT links.board_1 AS board_id, links.dir_1 AS direction
	FROM links JOIN bs ON links.board_1 IN (SELECT board_id FROM bs)
	WHERE links.live AND NOT links.board_2 IN (SELECT board_id FROM bs)
UNION
SELECT links.board_2 AS board_id, links.dir_2 AS direction
	FROM links JOIN bs ON links.board_2 IN (SELECT board_id FROM bs)
	WHERE links.live AND NOT links.board_1 IN (SELECT board_id FROM bs);
