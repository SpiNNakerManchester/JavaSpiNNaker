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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Objects.nonNull;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.JOB;
import static uk.ac.manchester.spinnaker.alloc.web.WebServiceComponentNames.MACH;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.manchester.spinnaker.alloc.allocator.SpallocAPI.BoardLocation;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

/**
 * Describes the result of a where-is style request.
 *
 * @author Donal Fellows
 */
@JsonInclude(NON_NULL)
public class WhereIsResponse {
	/** The ID of the job using the board, or {@code null}/absent if none. */
	public final Integer jobId;

	/** The URI to the job using the board, or {@code null}/absent if none. */
	public final URI jobRef;

	/**
	 * The location of the chip in the job using the board, or
	 * {@code null}/absent if none.
	 */
	public final ChipLocation jobChip;

	/**
	 * The location of the chip.
	 */
	public final ChipLocation chip;

	/** The logical (triad) coordinates of the board. */
	public final BoardCoordinates logicalBoardCoordinates;

	/** The name of the machine. */
	public final String machine;

	/** The URI to the machine. */
	public final URI machineRef;

	/** The chip location of the board's root. */
	public final ChipLocation boardChip;

	/** The physical (BMP+board) coordinates of the board. */
	public final BoardPhysicalCoordinates physicalBoardCoordinates;

	WhereIsResponse(BoardLocation location, UriInfo ui) {
		var minter = ui.getBaseUriBuilder().path("{major}/{minor}");
		machine = location.getMachine();
		machineRef = minter.build(MACH, machine);
		chip = location.getChip();
		boardChip = location.getBoardChip();
		logicalBoardCoordinates = location.getLogical();
		physicalBoardCoordinates = location.getPhysical();
		var j = location.getJob();
		if (nonNull(j)) {
			jobId = j.getId();
			jobRef = minter.build(JOB, jobId);
			jobChip = j.getRootChip().map(location::getChipRelativeTo)
					.orElse(null);
		} else {
			jobId = null;
			jobRef = null;
			jobChip = null;
		}
	}
}
