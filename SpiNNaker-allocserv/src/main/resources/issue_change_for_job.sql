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

INSERT INTO pending_changes(
	job_id, board_id,
	"power", fpga_n, fpga_s, fpga_e, fpga_w, fpga_nw, fpga_se)
SELECT
	VALUES (?, (
		SELECT board_id FROM boards
		WHERE machine_id = ? AND x = ? AND y = ?
			AND may_be_allocated > 0), ?, ?, ?, ?, ?, ?, ?);
