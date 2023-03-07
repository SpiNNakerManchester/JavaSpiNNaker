/*
 * Copyright (c) 2021 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.alloc.client;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static uk.ac.manchester.spinnaker.alloc.client.ClientUtils.readOnlyCopy;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.errorprone.annotations.Immutable;

import uk.ac.manchester.spinnaker.alloc.client.SpallocClient.Machine;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadDepth;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.spalloc.messages.BoardCoordinates;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;

/** A description of what boards have been allocated to a job. */
@JsonIgnoreProperties({
	"power", "machine", "machine-ref"
})
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
@JsonDeserialize(builder = AllocatedMachine.Builder.class)
public final class AllocatedMachine {
	@ValidTriadWidth
	private final int width;

	@ValidTriadHeight
	private final int height;

	@ValidTriadDepth
	private final int depth;

	@NotBlank
	private final String machineName;

	// Not final; set later
	private Machine machine;

	private final List<@Valid ConnectionInfo> connections;

	private final List<@Valid BoardCoordinates> boards;

	private AllocatedMachine(int width, int height, int depth,
			String machineName,
			List<ConnectionInfo> connections, List<BoardCoordinates> boards) {
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.machineName = machineName;
		this.connections = connections;
		this.boards = boards;
	}

	/** @return Rectangle width, in triads. */
	public int getWidth() {
		return width;
	}

	/** @return Rectangle height, in triads. */
	public int getHeight() {
		return height;
	}

	/**
	 * @return Depth of rectangle. 1 or 3. Only the single-board case uses a
	 *         depth of 1.
	 */
	public int getDepth() {
		return depth;
	}

	/** @return On what machine. */
	public String getMachineName() {
		return machineName;
	}

	/** @return The hosting SpiNNaker machine. */
	public Machine getMachine() {
		return machine;
	}

	void setMachine(Machine machine) {
		this.machine = machine;
	}

	/** @return How to talk to boards. */
	public List<ConnectionInfo> getConnections() {
		return connections;
	}

	/** @return Where the boards are. */
	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	/** Information about a connection to a board. */
	@JsonPropertyOrder({
		"chip", "hostname"
	})
	@JsonFormat(shape = ARRAY)
	@JsonAutoDetect(setterVisibility = NON_PRIVATE)
	@Immutable
	public static final class ConnectionInfo {
		@Valid
		private final ChipLocation chip;

		@IPAddress
		private final String hostname;

		/**
		 * @param chip
		 *            Which root chip (of a board) is this about?
		 * @param hostname
		 *            What's the IP address of the chip?
		 */
		public ConnectionInfo(@JsonProperty("chip") ChipLocation chip,
				@JsonProperty("hostname") String hostname) {
			this.chip = chip;
			this.hostname = hostname;
		}

		/** @return Which root chip (of a board) is this about? */
		public ChipLocation getChip() {
			return chip;
		}

		/** @return What's the IP address of the chip? */
		public String getHostname() {
			return hostname;
		}
	}

	@JsonIgnoreProperties({
		"power", "machine", "machine-ref"
	})
	@JsonPOJOBuilder
	static class Builder {
		private int width;

		private int height;

		private int depth;

		private String machineName;

		private List<ConnectionInfo> connections = List.of();

		private List<BoardCoordinates> boards = List.of();

		void withWidth(int width) {
			this.width = width;
		}

		void withHeight(int height) {
			this.height = height;
		}

		void withDepth(int depth) {
			this.depth = depth;
		}

		void withMachineName(String machineName) {
			this.machineName = machineName;
		}

		void withConnections(List<ConnectionInfo> connections) {
			this.connections = readOnlyCopy(connections);
		}

		void withBoards(List<BoardCoordinates> boards) {
			this.boards = readOnlyCopy(boards);
		}

		AllocatedMachine build() {
			return new AllocatedMachine(width, height, depth, machineName,
					connections, boards);
		}
	}
}
