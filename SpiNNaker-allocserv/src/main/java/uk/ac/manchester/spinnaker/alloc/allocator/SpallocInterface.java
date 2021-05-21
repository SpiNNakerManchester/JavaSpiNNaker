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
package uk.ac.manchester.spinnaker.alloc.allocator;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.alloc.allocator.Epochs.JobsEpoch;
import uk.ac.manchester.spinnaker.alloc.allocator.SpallocInterface.BoardLocation;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardPhysicalCoordinates;

public interface SpallocInterface {
	Map<String, Machine> getMachines() throws SQLException;

	Machine getMachine(String name) throws SQLException;

	Jobs getJobs(boolean deleted, int limit, int start) throws SQLException;

	Job getJob(int id) throws SQLException;

	Job createJob(String owner, List<Integer> dimensions, String machineName,
			List<String> tags, Integer maxDeadBoards) throws SQLException;

	/**
	 * Describes a particular job known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Job {
		void access(String keepaliveAddress) throws SQLException;

		void destroy(String reason) throws SQLException;

		void waitForChange(long timeout);

		int getId();

		JobState getState();

		Float getStartTime();

		String getReason();

		String getKeepaliveHost();

		SubMachine getMachine();

		BoardLocation whereIs(int x, int y);

		ChipLocation getRootChip();

		String getOwner();

		Integer getWidth();

		Integer getHeight();
	}

	/**
	 * Describes list set of jobs known to the allocator.
	 *
	 * @author Donal Fellows
	 */
	interface Jobs {
		void waitForChange(long timeout);

		List<Integer> ids(int start, int limit);
	}

	/**
	 * Describes a particular machine known to the allocator.
	 * Must implement equality by ID or name (both are unique).
	 *
	 * @author Donal Fellows
	 */
	interface Machine {
		/** The ID of the machine. Unique. */
		int getId();

		/** The name of the machine. Unique. */
		String getName();

		/** The tags associated with the machine. */
		List<String> getTags();

		/** The width of the machine. */
		int getWidth();

		/** The height of the machine. */
		int getHeight();

		// TODO: dead boards, dead links

		void waitForChange(long timeout);

		BoardLocation getBoardByChip(int x, int y, JobsEpoch je)
				throws SQLException;

		BoardLocation getBoardByPhysicalCoords(int cabinet, int frame,
				int board, JobsEpoch je) throws SQLException;

		BoardLocation getBoardByLogicalCoords(int x, int y, int z, JobsEpoch je)
				throws SQLException;

		String getRootBoardBMPAddress() throws SQLException;

		List<Integer> getBoardNumbers() throws SQLException;
	}

	/**
	 * Describes the locations of boards in a machine.
	 *
	 * @author Donal Fellows
	 */
	interface BoardLocation {
		ChipLocation getBoardChip();

		ChipLocation getChipRelativeTo(ChipLocation rootChip);

		/**
		 * What machine is the board on?
		 *
		 * @return name of machine
		 */
		String getMachine();

		/** Where is the board logically within its machine? */
		BoardCoordinates getLogical();

		/** Where is the board physically in its machine? */
		BoardPhysicalCoordinates getPhysical();

		/**
		 * Where is the chip of interest? Usually the root chip of the board.
		 */
		ChipLocation getChip();

		/**
		 * What job is the board allocated to? May be {@code null} for an
		 * unallocated board.
		 */
		Job getJob();
	}
}
