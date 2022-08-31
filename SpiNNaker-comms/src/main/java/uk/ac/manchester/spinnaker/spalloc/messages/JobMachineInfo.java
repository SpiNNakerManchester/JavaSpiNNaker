/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.isNull;

import java.util.List;

/**
 * A description of a machine associated with a job, in terms of width, height,
 * connections and its name.
 */
public final class JobMachineInfo {
	private int width;

	private int height;

	private List<Connection> connections = List.of();

	private String machineName;

	private List<BoardCoordinates> boards = List.of();

	/** Number of boards/Connections to list individually in the toString. */
	private static final int PRINT_CONNECTIONS_THRESHOLD = 6;

	/** @return The width of the allocated machine chunk. */
	public int getWidth() {
		return width;
	}

	/** @param width The width of the allocated machine chunk. */
	public void setWidth(int width) {
		this.width = width;
	}

	/** @return The height of the allocated machine chunk. */
	public int getHeight() {
		return height;
	}

	/** @param height The height of the allocated machine chunk. */
	public void setHeight(int height) {
		this.height = height;
	}

	/** @return How to talk to the allocated boards. */
	public List<Connection> getConnections() {
		return connections;
	}

	/** @param connections How to talk to the allocated boards. */
	public void setConnections(List<Connection> connections) {
		this.connections = isNull(connections) ? List.of()
				: unmodifiableList(connections);
	}

	/** @return The name of the machine handling the job. */
	public String getMachineName() {
		return machineName;
	}

	/** @param machineName The name of the machine handling the job. */
	public void setMachineName(String machineName) {
		this.machineName = machineName;
	}

	/** @return Locations of the allocated boards. */
	public List<BoardCoordinates> getBoards() {
		return boards;
	}

	/** @param boards Locations of the allocated boards. */
	public void setBoards(List<BoardCoordinates> boards) {
		this.boards = isNull(boards) ? List.of() : unmodifiableList(boards);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder();
		builder.append("width: ").append(width);
		builder.append(" height: ").append(height);
		builder.append(" machineName: ").append(machineName);
		if (connections.size() > PRINT_CONNECTIONS_THRESHOLD
				&& boards.size() == connections.size()) {
			builder.append(" # connections/boards: ")
					.append(connections.size());
		} else {
			builder.append(" connections: ").append(connections);
			builder.append(" boards: ").append(boards);
		}
		return builder.toString();
	}
}
