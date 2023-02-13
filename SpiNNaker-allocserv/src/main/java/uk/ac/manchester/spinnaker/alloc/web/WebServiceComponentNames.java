/*
 * Copyright (c) 2021-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.web;

/**
 * Names of parts of this web service.
 *
 * @author Donal Fellows
 */
public interface WebServiceComponentNames {
	/** The overall service name in URL space. */
	String SERV = "spalloc";

	/** The machine resource name component. */
	String MACH = "machines";

	/** The machine logical board resource name component. */
	String MACH_BOARD_BY_LOGICAL = "logical-board";

	/** The machine physical board resource name component. */
	String MACH_BOARD_BY_PHYSICAL = "physical-board";

	/** The machine chip resource name component. */
	String MACH_BOARD_BY_CHIP = "chip";

	/** The machine board IP address resource name component. */
	String MACH_BOARD_BY_ADDRESS = "board-ip";

	/** The job resource name component. */
	String JOB = "jobs";

	/** The job keep-alive resource name component. */
	String JOB_KEEPALIVE = "keepalive";

	/** The job machine resource name component. */
	String JOB_MACHINE = "machine";

	/** The job power resource name component. */
	String JOB_MACHINE_POWER = "power";

	/** The job board-by-chip resource name component. */
	String JOB_BOARD_BY_CHIP = "chip";

	/** The job report-an-issue-with-a-board resource name component. */
	String REPORT_ISSUE = "report-issue";

	/** The wait-for-change query parameter name. */
	String WAIT = "wait";

	/** The ID path parameter name. */
	String ID = "id";

	/** The name path parameter name. */
	String NAME = "name";

	/** The X coordinate query parameter name. */
	String CHIP_X = "x";

	/** The Y coordinate query parameter name. */
	String CHIP_Y = "y";
}
