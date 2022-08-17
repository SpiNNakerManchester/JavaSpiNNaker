/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
