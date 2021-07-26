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
package uk.ac.manchester.spinnaker.allocator;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.Version;

public interface SpallocClient {
	Version getVersion() throws IOException;

	List<Machine> listMachines() throws IOException;

	List<Job> listJobs() throws IOException;

	interface Machine {
		/** @return The name of the machine. */
		String getName();

		/** @return The tags of the machine. */
		List<String> getTags();

		/** @return The width of the machine, in triads. */
		int getWidth();

		/** @return The height of the machine, in triads. */
		int getHeight();

		/** @return The (estimated) number of live boards in the machine. */
		int getLiveBoardCount();

		WhereIs getBoardByTriad(int x, int y, int z) throws IOException;

		WhereIs getBoardByPhysicalCoords(int cabinet, int frame, int board)
				throws IOException;

		WhereIs getBoardByChip(HasChipLocation chip) throws IOException;

		WhereIs getBoardByIPAddress(String address) throws IOException;
	}

	interface Job {
		String describe() throws IOException;

		void keepalive() throws IOException;

		void delete(String reason) throws IOException;

		String machine() throws IOException;

		boolean getPower() throws IOException;

		void setPower(boolean switchOn) throws IOException;

		WhereIs whereIs(HasChipLocation chip) throws IOException;
	}

	@JsonFormat(shape = Shape.ARRAY)
	class Triad {
		public int x;
		public int y;
		public int z;

		@Override
		public String toString() {
			return String.format("[X:%d, Y:%d, Z:%d]", x,y,z);
		}
	}

	@JsonFormat(shape = Shape.ARRAY)
	class Physical {
		public int cabinet;
		public int frame;
		public int board;

		@Override
		public String toString() {
			return String.format("[%d:%d:%d]", cabinet, frame ,board);
		}
	}

	class WhereIs {
		@JsonAlias("job-id")
		public Integer jobId;

		@JsonAlias("job-ref")
		public URI jobRef;

		@JsonAlias("job-chip")
		public ChipLocation jobChip;

		public ChipLocation chip;

		@JsonIgnore
		public Machine machineHandle;

		@JsonAlias("machine")
		public String machineName;

		@JsonAlias("machine-ref")
		public URI machineRef;

		@JsonAlias("board-chip")
		public ChipLocation boardChip;

		@JsonAlias("logical-board-coordinates")
		public Triad logicalCoords;

		@JsonAlias("physical-board-coordinates")
		public Physical physicalCoords;
	}
}
