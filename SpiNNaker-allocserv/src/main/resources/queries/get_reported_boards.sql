-- Copyright (c) 2021-2023 The University of Manchester
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

-- Get boards with more problem reports than a critical threshold
WITH report_counts AS (
	SELECT
		board_reports.board_id,
		COUNT(board_reports.report_id) AS num_reports
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
