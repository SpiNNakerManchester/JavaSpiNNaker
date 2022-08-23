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

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.allocator.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;

/** A description of what boards have been allocated to a job. */
@JsonIgnoreProperties({
	"power", "machine", "machine-ref"
})
public class AllocatedMachine {
	private int width;

	private int height;

	private int depth;

	private String machineName;

	private Machine machine;

	private List<ConnectionInfo> connections;

	private List<BoardCoordinates> boards;

	/** @return Rectangle width. */
	public int getWidth() {
		return width;
	}

	/** @param width Rectangle width. */
	public void setWidth(int width) {
		this.width = width;
	}

	/** @return Rectangle height. */
	public int getHeight() {
		return height;
	}

	/** @param height Rectangle height. */
	public void setHeight(int height) {
		this.height = height;
	}

	/** @return Depth of rectangle. 1 or 3. */
	public int getDepth() {
		return depth;
	}

	/** @param depth Depth of rectangle. 1 or 3. */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	/** @return On what machine. */
	public String getMachineName() {
		return machineName;
	}

	/** @param machineName On what machine. */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/** @return The hosting SpiNNaker machine. */
	public Machine getMachine() {
		return machine;
	}

	/** @param machine The hosting SpiNNaker machine. */
	void setMachine(Machine machine) {
		this.machine = machine;
	}

	/** @return How to talk to boards. */
	public List<ConnectionInfo> getConnections() {
		return connections;
	}

	/** @param connections How to talk to boards. */
	public void setConnections(List<ConnectionInfo> connections) {
		this.connections = connections;
	}

	/** @return Where the boards are. */
	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	/** @param boards Where the boards are. */
	public void setBoards(List<BoardCoordinates> boards) {
		this.boards = boards;
	}

	/** Information about a connection to a board. */
	@JsonPropertyOrder({
		"chip", "hostname"
	})
	@JsonFormat(shape = ARRAY)
	public static class ConnectionInfo {
		private ChipLocation chip;

		private String hostname;

		/** @return Which root chip (of a board) is this about? */
		public ChipLocation getChip() {
			return chip;
		}

		/** @param chip Which root chip (of a board) is this about? */
		public void setChip(ChipLocation chip) {
			this.chip = chip;
		}

		/** @return What's the IP address of the chip? */
		public String getHostname() {
			return hostname;
		}

		/** @param hostname What's the IP address of the chip? */
		public void setHostname(String hostname) {
			this.hostname = hostname;
		}
	}
}
