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

INSERT INTO pending_changes(
	job_id, board_id, from_state, to_state,
	power, fpga_n, fpga_e, fpga_se, fpga_s, fpga_w, fpga_nw)
VALUES (
	:job_id, :board_id, :from_state, :to_state,
	:power, :fpga_n, :fpga_e, :fpga_se, :fpga_s, :fpga_w, :fpga_nw);
