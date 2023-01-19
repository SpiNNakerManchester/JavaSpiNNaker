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

-- Get boards with more problem reports than a critical threshold
WITH report_counts AS (
	SELECT
		board_reports.board_id,
		COUNT(board_report.report_id) AS num_reports
	FROM board_reports
	JOIN boards USING (board_id)
	WHERE boards.functioning IS NOT 0 -- Ignore disabled boards
	GROUP BY board_id)
SELECT
    boards.board_id,
    report_counts.num_reports,
    boards.x,
    boards.y,
    boards.z,
    boards.address
FROM
	report_counts
	JOIN boards USING (board_id)
WHERE report_counts.num_reports >= :threshold;
