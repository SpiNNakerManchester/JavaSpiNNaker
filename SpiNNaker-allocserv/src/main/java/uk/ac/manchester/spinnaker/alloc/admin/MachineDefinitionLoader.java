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
package uk.ac.manchester.spinnaker.alloc.admin;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.transaction;
import static uk.ac.manchester.spinnaker.alloc.DatabaseEngine.update;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.DatabaseEngine;
import uk.ac.manchester.spinnaker.alloc.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 * Loads definitions of machines from JSON, as generated by {@code py2json}.
 *
 * @author Donal Fellows
 */
@Component
public class MachineDefinitionLoader {
	private static final int DECIMAL = 10;

	/**
	 * Triad coordinates.
	 *
	 * @author Donal Fellows
	 */
	public static final class TriadCoords implements Comparable<TriadCoords> {
		/** X coordinate. */
		public final int x;

		/** Y coordinate. */
		public final int y;

		/** Z coordinate. */
		public final int z;

		@JsonCreator
		public TriadCoords(@JsonProperty("x") int x, @JsonProperty("y") int y,
				@JsonProperty("z") int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private static final Pattern PATTERN;

		static {
			PATTERN = Pattern.compile("^\\[x:(\\d+),y:(\\d+),z:(\\d+)\\]$");
		}

		@JsonCreator
		public TriadCoords(String serialForm) {
			Matcher m = PATTERN.matcher(serialForm);
			if (!m.matches()) {
				throw new IllegalArgumentException(
						"bad argument: " + serialForm);
			}
			int idx = 0;
			x = Integer.parseInt(m.group(++idx), DECIMAL);
			y = Integer.parseInt(m.group(++idx), DECIMAL);
			z = Integer.parseInt(m.group(++idx), DECIMAL);
		}

		private static final int TRIAD_SIZE = 12;

		private static final int TRIAD_MAJOR_OFFSET = 8;

		private static final int TRIAD_MINOR_OFFSET = 4;

		ChipLocation chipLocation() {
			int rootX = x * TRIAD_SIZE;
			int rootY = y * TRIAD_SIZE;
			switch (z) {
			case 0:
				break;
			case 1:
				rootX += TRIAD_MAJOR_OFFSET;
				rootY += TRIAD_MINOR_OFFSET;
				break;
			case 2:
				rootX += TRIAD_MINOR_OFFSET;
				rootY += TRIAD_MAJOR_OFFSET;
				break;
			default:
				throw new IllegalArgumentException("bad Z coordinate");
			}
			return new ChipLocation(rootX, rootY);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof TriadCoords) {
				TriadCoords other = (TriadCoords) obj;
				return x == other.x && y == other.y && z == other.z;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (((x << 2 + x) ^ y) << 2 + y) ^ z;
		}

		@Override
		public String toString() {
			return "[x:" + x + ",y:" + y + ",z:" + z + "]";
		}

		@Override
		public int compareTo(TriadCoords other) {
			int cmp = Integer.compare(x, other.x);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Integer.compare(y, other.y);
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(z, other.z);
		}
	}

	/**
	 * Frame/BMP coordinates.
	 *
	 * @author Donal Fellows
	 */
	public static final class BMPCoords implements Comparable<BMPCoords> {
		/** Cabinet number. */
		public final int c;

		/** Frame number. */
		public final int f;

		public BMPCoords(int c, int f) {
			this.c = c;
			this.f = f;
		}

		private static final Pattern PATTERN;

		static {
			PATTERN = Pattern.compile("^\\[c:(\\d+),f:(\\d+)\\]$");
		}

		public BMPCoords(String serialForm) {
			Matcher m = PATTERN.matcher(serialForm);
			if (!m.matches()) {
				throw new IllegalArgumentException(
						"bad argument: " + serialForm);
			}
			int idx = 0;
			c = Integer.parseInt(m.group(++idx), DECIMAL);
			f = Integer.parseInt(m.group(++idx), DECIMAL);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BMPCoords) {
				BMPCoords other = (BMPCoords) obj;
				return c == other.c && f == other.f;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return ((c << 2 + c) ^ f) << 2 + f;
		}

		@Override
		public String toString() {
			return "[c:" + c + ",f:" + f + "]";
		}

		@Override
		public int compareTo(BMPCoords other) {
			int cmp = Integer.compare(c, other.c);
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(f, other.f);
		}
	}

	/**
	 * Physical board coordinates.
	 *
	 * @author Donal Fellows
	 */
	public static final class BoardPhysicalCoords
			implements Comparable<BoardPhysicalCoords> {
		/** Cabinet number. */
		public final int c;

		/** Frame number. */
		public final int f;

		/** Board number. */
		public final int b;

		@JsonCreator
		public BoardPhysicalCoords(@JsonProperty("c") int c,
				@JsonProperty("f") int f, @JsonProperty("b") int b) {
			this.c = c;
			this.f = f;
			this.b = b;
		}

		private static final Pattern PATTERN;

		static {
			PATTERN = Pattern.compile("^\\[c:(\\d+),f:(\\d+),b:(\\d+)\\]$");
		}

		@JsonCreator
		public BoardPhysicalCoords(String serialForm) {
			Matcher m = PATTERN.matcher(serialForm);
			if (!m.matches()) {
				throw new IllegalArgumentException(
						"bad argument: " + serialForm);
			}
			int idx = 0;
			c = Integer.parseInt(m.group(++idx), DECIMAL);
			f = Integer.parseInt(m.group(++idx), DECIMAL);
			b = Integer.parseInt(m.group(++idx), DECIMAL);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof BoardPhysicalCoords) {
				BoardPhysicalCoords other = (BoardPhysicalCoords) obj;
				return c == other.c && f == other.f && b == other.b;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return (((c << 2 + c) ^ f) << 2 + f) ^ b;
		}

		@Override
		public String toString() {
			return "[c:" + c + ",f:" + f + ",b:" + b + "]";
		}

		BMPCoords bmp() {
			return new BMPCoords(c, f);
		}

		@Override
		public int compareTo(BoardPhysicalCoords other) {
			int cmp = Integer.compare(c, other.c);
			if (cmp != 0) {
				return cmp;
			}
			cmp = Integer.compare(f, other.f);
			if (cmp != 0) {
				return cmp;
			}
			return Integer.compare(b, other.b);
		}
	}

	/**
	 * Enumeration of links from a SpiNNaker chip, as used in the old spalloc.
	 * <p>
	 * Note that the numbers chosen have two useful properties:
	 * <p>
	 * <ul>
	 * <li>The integer values assigned are chosen to match the numbers used to
	 * identify the links in the low-level software API and hardware registers.
	 * <li>The links are ordered consecutively in anticlockwise order meaning
	 * the opposite link is {@code (link+3)%6}.
	 * </ul>
	 *
	 * @author Donal Fellows
	 */
	public enum Link {
		/** East. */
		east,
		/** North-East. */
		northEast,
		/** North. */
		north,
		/** West. */
		west,
		/** South-West. */
		southWest,
		/** South. */
		south
	}

	/**
	 * A machine description. JSON-serializable.
	 *
	 * @author Donal Fellows
	 */
	@JsonDeserialize(builder = Machine.Builder.class)
	public static final class Machine {
		private String name;

		private Set<String> tags;

		private int width;

		private int height;

		private Set<TriadCoords> deadBoards;

		private Map<TriadCoords, EnumSet<Link>> deadLinks;

		private Map<TriadCoords, BoardPhysicalCoords> boardLocations;

		private Map<BMPCoords, String> bmpIPs;

		private Map<TriadCoords, String> spinnakerIPs;

		/** @return The name of the machine. */
		public String getName() {
			return name;
		}

		/** @return The tags of the machine. */
		public Set<String> getTags() {
			return unmodifiableSet(tags);
		}

		/** @return The width of the machine, in triads. */
		public int getWidth() {
			return width;
		}

		/** @return The height of the machine, in triads. */
		public int getHeight() {
			return height;
		}

		/** @return The dead boards of the machine. */
		public Set<TriadCoords> getDeadBoards() {
			return unmodifiableSet(deadBoards);
		}

		/**
		 * @return The extra dead links of the machine. Doesn't include links to
		 *         dead boards.
		 */
		public Map<TriadCoords, EnumSet<Link>> getDeadLinks() {
			return unmodifiableMap(deadLinks);
		}

		/** @return The logical-to-physical board location map. */
		public Map<TriadCoords, BoardPhysicalCoords> getBoardLocations() {
			return unmodifiableMap(boardLocations);
		}

		/** @return The IP addresses of the BMPs. */
		public Map<BMPCoords, String> getBmpIPs() {
			return unmodifiableMap(bmpIPs);
		}

		/** @return The IP addresses of the boards. */
		public Map<TriadCoords, String> getSpinnakerIPs() {
			return unmodifiableMap(spinnakerIPs);
		}

		@Override
		public String toString() {
			return new StringBuilder("Machine(").append("name=").append(name)
					.append(",").append("tags=").append(tags).append(",")
					.append("width=").append(width).append(",")
					.append("height=").append(height).append(",")
					.append("deadBoards=").append(deadBoards).append(",")
					.append("deadLinks=").append(deadLinks).append(",")
					.append("boardLocations=").append(boardLocations)
					.append(",").append("bmpIPs=").append(bmpIPs).append(",")
					.append("spinnakerIPs=").append(spinnakerIPs).append(")")
					.toString();
		}

		@JsonPOJOBuilder
		static class Builder {
			private String name;

			private Set<String> tags;

			private int width;

			private int height;

			private Set<TriadCoords> deadBoards;

			private Map<TriadCoords, EnumSet<Link>> deadLinks;

			private Map<TriadCoords, BoardPhysicalCoords> boardLocations;

			private Map<BMPCoords, String> bmpIps;

			private Map<TriadCoords, String> spinnakerIps;

			public Builder withName(String name) {
				this.name = name;
				return this;
			}

			public Builder withTags(Set<String> tags) {
				this.tags = tags;
				return this;
			}

			public Builder withWidth(int width) {
				this.width = width;
				return this;
			}

			public Builder withHeight(int height) {
				this.height = height;
				return this;
			}

			public Builder withDeadBoards(Set<TriadCoords> deadBoards) {
				this.deadBoards = deadBoards;
				return this;
			}

			public Builder withDeadLinks(
					Map<TriadCoords, EnumSet<Link>> deadLinks) {
				this.deadLinks = deadLinks;
				return this;
			}

			public Builder withBoardLocations(
					Map<TriadCoords, BoardPhysicalCoords> boardLocations) {
				this.boardLocations = boardLocations;
				return this;
			}

			public Builder withBmpIps(Map<BMPCoords, String> bmpIps) {
				this.bmpIps = bmpIps;
				return this;
			}

			public Builder withSpinnakerIps(
					Map<TriadCoords, String> spinnakerIps) {
				this.spinnakerIps = spinnakerIps;
				return this;
			}

			public Machine build() {
				Machine m = new Machine();
				m.name = name;
				m.tags = tags;
				m.width = width;
				m.height = height;
				m.deadBoards = deadBoards;
				m.deadLinks = deadLinks;
				m.boardLocations = boardLocations;
				m.bmpIPs = bmpIps;
				m.spinnakerIPs = spinnakerIps;
				return m;
			}
		}
	}

	/**
	 * A configuration description. JSON-serializable. Largely ignored as it
	 * represents configuration settings that we handle elsewhere. However, the
	 * {@code machines} property <em>is</em> interesting.
	 *
	 * @author Donal Fellows
	 */
	static final class Configuration {
		private List<Machine> machines;

		private int port;

		private String ip;

		private double timeoutCheckInterval;

		private int maxRetiredJobs;

		private int secondsBeforeFree;

		/** @return The machines to manage. */
		public List<Machine> getMachines() {
			return unmodifiableList(machines);
		}

		public void setMachines(List<Machine> machines) {
			this.machines = machines;
		}

		/** @return The port for the service to listen on. */
		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		/**
		 * @return The host address for the service to listen on. Empty = all
		 *         interfaces.
		 */
		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		/** @return How often (in seconds) to check for timeouts. */
		public double getTimeoutCheckInterval() {
			return timeoutCheckInterval;
		}

		public void setTimeoutCheckInterval(double timeoutCheckInterval) {
			this.timeoutCheckInterval = timeoutCheckInterval;
		}

		/** @return How many retired jobs to retain. */
		public int getMaxRetiredJobs() {
			return maxRetiredJobs;
		}

		public void setMaxRetiredJobs(int maxRetiredJobs) {
			this.maxRetiredJobs = maxRetiredJobs;
		}

		/** @return Time to wait before freeing. */
		public int getSecondsBeforeFree() {
			return secondsBeforeFree;
		}

		public void setSecondsBeforeFree(int secondsBeforeFree) {
			this.secondsBeforeFree = secondsBeforeFree;
		}

		@Override
		public String toString() {
			return new StringBuilder("Configuration(").append("machines=")
					.append(machines).append(",").append("port=").append(port)
					.append(",").append("ip=").append(ip).append(",")
					.append("timeoutCheckInterval=")
					.append(timeoutCheckInterval).append(",")
					.append("maxRetiredJobs=").append(maxRetiredJobs)
					.append(",").append("secondsBeforeFree=")
					.append(secondsBeforeFree).append(")").toString();
		}
	}

	@Autowired
	private DatabaseEngine db;

	@Autowired
	private JsonMapper mapper;

	/**
	 * Read a JSON-converted traditional spalloc configuration and get the
	 * machine definitions from inside.
	 *
	 * @param file
	 *            The file of JSON.
	 * @return The machines from that file.
	 * @throws IOException
	 *             If anything goes wrong.
	 * @throws JsonParseException
	 *             if underlying input contains invalid JSON content
	 * @throws JsonMappingException
	 *             if the input JSON structure does not match the structure of
	 *             JSONified spalloc configuration
	 */
	public List<Machine> readMachineDefinitions(File file)
			throws IOException, JsonParseException, JsonMappingException {
		return mapper.readValue(file, Configuration.class).getMachines();
	}

	static class Q implements AutoCloseable {
		final Update makeMachine;
		final Update makeTag;
		final Update makeBMP;
		final Update makeBoard;

		Q(Connection conn) throws SQLException {
			this.makeMachine = update(conn,
					"INSERT INTO machines(machine_name, "
							+ "width, height, depth, board_model) "
							+ "VALUES(:name, :width, :height, :depth, 5)");
			this.makeTag = update(conn, "INSERT INTO tags(machine_id, tag) "
					+ "VALUES(:machine_id, :tag)");
			this.makeBMP = update(conn,
					"INSERT INTO bmp(machine_id, address, cabinet, frame) "
							+ "VALUES(:machine_id, :address, :cabinet, "
							+ ":frame)");
			this.makeBoard = update(conn, "INSERT INTO boards("
					+ "address, bmp_id, board_num, machine_id, x, y, z, "
					+ "root_x, root_y, functioning) VALUES("
					+ ":address, :bmp_id, :board, :machine_id, :x, :y, :z, "
					+ ":root_x, :root_y, :enabled)");
		}

		@Override
		public void close() throws SQLException {
			makeMachine.close();
			makeTag.close();
			makeBMP.close();
			makeBoard.close();
		}
	}

	public void loadMachineDefinitions(File file) throws SQLException,
			JsonParseException, JsonMappingException, IOException {
		List<Machine> machines = readMachineDefinitions(file);
		try (Connection conn = db.getConnection(); Q queries = new Q(conn)) {
			for (Machine machine : machines) {
				transaction(conn,
						() -> loadMachineDefinition(queries, machine));
			}
		}
	}

	static class InsertFailedException extends SQLException {
		private static final long serialVersionUID = -4930512416142843777L;

		InsertFailedException(String table) {
			super("could not insert into " + table);
		}
	}

	void loadMachineDefinition(Q queries, Machine machine) throws SQLException {
		int depth = (machine.boardLocations.size() == 1) ? 1 : 3;
		int machineId = queries.makeMachine.key(machine.getName(),
				machine.getWidth(), machine.getHeight(), depth)
				.orElseThrow(() -> new InsertFailedException("machines"));
		for (String tag : machine.getTags()) {
			queries.makeTag.key(machineId, tag);
		}
		Map<BMPCoords, Integer> bmpIds = new HashMap<>();
		for (Entry<BMPCoords, String> entry : machine.bmpIPs.entrySet()) {
			queries.makeBMP
					.key(machineId, entry.getValue(), entry.getKey().c,
							entry.getKey().f)
					.ifPresent(id -> bmpIds.put(entry.getKey(), id));
		}
		Map<TriadCoords, Integer> boardIds = new HashMap<>();
		for (Entry<TriadCoords,
				BoardPhysicalCoords> entry : machine.boardLocations
						.entrySet()) {
			TriadCoords triad = entry.getKey();
			BoardPhysicalCoords phys = entry.getValue();
			int bmpID = bmpIds.get(phys.bmp());
			String addr = machine.spinnakerIPs.get(triad);
			ChipLocation root = triad.chipLocation();
			queries.makeBoard
					.key(addr, bmpID, phys.b, machineId, triad.x, triad.y,
							triad.z, root.getX(), root.getY(), true)
					.ifPresent(id -> boardIds.put(triad, id));
		}
		// TODO Generate links
	}
}
