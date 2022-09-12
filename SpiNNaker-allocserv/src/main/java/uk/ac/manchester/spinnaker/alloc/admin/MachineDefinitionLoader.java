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

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_CHIP_SIZE;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_DEPTH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.AssertFalse;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.sqlite.SQLiteException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.model.IPAddress;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.Direction;
import uk.ac.manchester.spinnaker.machine.board.Link;

/**
 * Loads definitions of machines from JSON, as generated by {@code py2json}.
 *
 * @author Donal Fellows
 */
@Service
public class MachineDefinitionLoader extends DatabaseAwareBean {
	private static final Logger log = getLogger(MachineDefinitionLoader.class);

	private static final int DECIMAL = 10;

	/**
	 * Parse a <em>decimal</em> integer.
	 *
	 * @param string
	 *            The string containing the number to parse.
	 * @return The parsed number.
	 * @throws NumberFormatException
	 *             If the string doesn't contain such a number.
	 */
	static int parseDec(String string) throws NumberFormatException {
		return parseInt(string, DECIMAL);
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
		@NotBlank(message = "machines must have real names")
		public String getName() {
			return name;
		}

		/** @return The tags of the machine. */
		public Set<@NotBlank String> getTags() {
			return unmodifiableSet(tags);
		}

		/** @return The width of the machine, in triads. */
		@Positive(message = "machine width must be greater than zero")
		public int getWidth() {
			return width;
		}

		/** @return The height of the machine, in triads. */
		@Positive(message = "machine height must be greater than zero")
		public int getHeight() {
			return height;
		}

		/** @return The depth of the machine, the number of boards per triad. */
		public int getDepth() {
			return boardLocations.size() == 1 ? 1 : TRIAD_DEPTH;
		}

		/** @return The dead boards of the machine. */
		public Set<@Valid TriadCoords> getDeadBoards() {
			return unmodifiableSet(deadBoards);
		}

		/**
		 * @return The extra dead links of the machine. Doesn't include links to
		 *         dead boards.
		 */
		public Map<@Valid TriadCoords, @NotNull EnumSet<Link>> getDeadLinks() {
			return unmodifiableMap(deadLinks);
		}

		/** @return The logical-to-physical board location map. */
		public Map<@Valid TriadCoords, @Valid BoardPhysicalCoords>
				getBoardLocations() {
			return unmodifiableMap(boardLocations);
		}

		/** @return The IP addresses of the BMPs. */
		public Map<@Valid BMPCoords, @IPAddress String> getBmpIPs() {
			return unmodifiableMap(bmpIPs);
		}

		/** @return The IP addresses of the boards. */
		public Map<@Valid TriadCoords, @IPAddress String> getSpinnakerIPs() {
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

		@JsonIgnore
		@AssertFalse(message = "machine name must not contain braces or spaces")
		private boolean isBadMachineName() {
			return badName(name);
		}

		@JsonIgnore
		@AssertFalse(message = "tag must not contain braces or spaces")
		private boolean isBadTag() {
			return tags.stream().anyMatch(Machine::badName);
		}

		private static boolean badName(String name) {
			return name.contains("{") || name.contains("}")
					|| name.contains("\u0000")
					|| name.codePoints().anyMatch(Character::isWhitespace);
		}

		@JsonIgnore
		@AssertTrue(message = "all boards must have sane logical coordinates")
		private boolean isCoordinateSane() {
			return boardLocations.keySet().stream().allMatch(loc -> (loc.x >= 0)
					&& (loc.x < width) && (loc.y >= 0) && (loc.y < height));
		}

		@JsonIgnore
		@AssertTrue(message = "all boards must have addresses")
		private boolean isNetworkSane() {
			return spinnakerIPs.size() == boardLocations.size();
		}

		@JsonIgnore
		@AssertTrue(message = "all boards must have BMPs")
		private boolean isBMPSane() {
			return boardLocations.values().stream()
					.allMatch(loc -> bmpIPs.containsKey(loc.bmp()));
		}

		/**
		 * Does a board have a dead link in a given direction?
		 *
		 * @param board
		 *            The location of the board
		 * @param direction
		 *            The direction asking about
		 * @return True iff the board at the given location has a dead link in
		 *         the given direction. Note that if the board doesn't exist at
		 *         all, this returns false.
		 */
		boolean hasDeadLinkAt(TriadCoords board, Direction direction) {
			if (deadLinks.isEmpty()) {
				return false;
			}
			return deadLinks.getOrDefault(board, EnumSet.noneOf(Link.class))
					.contains(Link.of(direction));
		}

		@JsonPOJOBuilder
		static class Builder {
			private String name;

			private Set<String> tags = Set.of();

			private int width;

			private int height;

			private Set<TriadCoords> deadBoards = Set.of();

			private Map<TriadCoords, EnumSet<Link>> deadLinks = Map.of();

			private Map<TriadCoords, BoardPhysicalCoords> boardLocations =
					Map.of();

			private Map<BMPCoords, String> bmpIps = Map.of();

			private Map<TriadCoords, String> spinnakerIps = Map.of();

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

			public Builder
					withDeadLinks(Map<TriadCoords, EnumSet<Link>> deadLinks) {
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

			public Builder
					withSpinnakerIps(Map<TriadCoords, String> spinnakerIps) {
				this.spinnakerIps = spinnakerIps;
				return this;
			}

			public Machine build() {
				var m = new Machine();
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
		public @NotNull List<@Valid Machine> getMachines() {
			return unmodifiableList(machines);
		}

		public void setMachines(List<Machine> machines) {
			this.machines = machines;
		}

		/** @return The port for the service to listen on. (Ignored) */
		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		/**
		 * @return The host address for the service to listen on. Empty = all
		 *         interfaces. (Ignored)
		 */
		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		/** @return How often (in seconds) to check for timeouts. (Ignored) */
		public double getTimeoutCheckInterval() {
			return timeoutCheckInterval;
		}

		public void setTimeoutCheckInterval(double timeoutCheckInterval) {
			this.timeoutCheckInterval = timeoutCheckInterval;
		}

		/** @return How many retired jobs to retain. (Ignored) */
		public int getMaxRetiredJobs() {
			return maxRetiredJobs;
		}

		public void setMaxRetiredJobs(int maxRetiredJobs) {
			this.maxRetiredJobs = maxRetiredJobs;
		}

		/** @return Time to wait before freeing. (Ignored) */
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
	private JsonMapper mapper;

	@Autowired
	private ValidatorFactory validatorFactory;

	@PostConstruct
	private void setUp() {
		try (var conn = getConnection()) {
			DirInfo.load(conn);
		}
	}

	/**
	 * Read a JSON-converted traditional spalloc configuration and get the
	 * machine definitions from inside.
	 *
	 * @param file
	 *            The file of JSON.
	 * @return The machines from that file.
	 * @throws IOException
	 *             If anything goes wrong with file access.
	 * @throws JsonParseException
	 *             if underlying input contains invalid JSON content
	 * @throws JsonMappingException
	 *             if the input JSON structure does not match the structure of
	 *             JSONified spalloc configuration
	 */
	public List<Machine> readMachineDefinitions(File file)
			throws IOException, JsonParseException, JsonMappingException {
		var cfg = mapper.readValue(file, Configuration.class);
		validate(cfg);
		return cfg.getMachines();
	}

	/**
	 * Read a JSON-converted traditional spalloc configuration and get the
	 * machine definitions from inside.
	 *
	 * @param stream
	 *            The stream of JSON.
	 * @return The machines from that stream.
	 * @throws IOException
	 *             If anything goes wrong with file access.
	 * @throws JsonParseException
	 *             if underlying input contains invalid JSON content
	 * @throws JsonMappingException
	 *             if the input JSON structure does not match the structure of
	 *             JSONified spalloc configuration
	 */
	public List<Machine> readMachineDefinitions(InputStream stream)
			throws IOException, JsonParseException, JsonMappingException {
		var cfg = mapper.readValue(stream, Configuration.class);
		validate(cfg);
		return cfg.getMachines();
	}

	/**
	 * Validates a configuration.
	 *
	 * @param cfg
	 *            The configuration to validate
	 * @throws IOException
	 *             If validation fails. Exception type chosen to fit with
	 *             signature of callers.
	 */
	private void validate(Configuration cfg) throws IOException {
		for (var violation : validatorFactory.getValidator().validate(cfg)) {
			// We ought to also say the other problems...
			throw new IOException("failed to validate configuration: "
					+ violation.getMessage());
		}
	}

	/**
	 * The various updates used when inserting a machine. This is
	 * connection-bound.
	 * <p>
	 * Factored out so they can be reused without needing masses of arguments.
	 * <p>
	 * Only non-{@code private} for testing purposes.
	 */
	private final class Updates extends AbstractSQL {
		private final Update makeMachine = conn.update(INSERT_MACHINE_SPINN_5);

		private final Update makeTag = conn.update(INSERT_TAG);

		private final Update makeBMP = conn.update(INSERT_BMP);

		private final Update makeBoard = conn.update(INSERT_BOARD);

		private final Update makeLink = conn.update(INSERT_LINK);

		private final Update setMaxCoords = conn.update(SET_MAX_COORDS);

		Updates() {
		}

		Updates(Connection c) {
			super(c);
		}

		@Override
		public void close() {
			makeMachine.close();
			makeTag.close();
			makeBMP.close();
			makeBoard.close();
			makeLink.close();
			setMaxCoords.close();
			super.close();
		}
	}

	/**
	 * Add the machine definitions in the given stream to the database.
	 *
	 * @param stream
	 *            The JSON configuration file.
	 * @throws JsonParseException
	 *             if underlying input contains invalid JSON content
	 * @throws JsonMappingException
	 *             if the input JSON structure does not match the structure of
	 *             JSONified spalloc configuration
	 * @throws IOException
	 *             If the file can't be read
	 */
	public void loadMachineDefinitions(InputStream stream)
			throws JsonParseException, JsonMappingException, IOException {
		var machines = readMachineDefinitions(stream);
		try (var sql = new Updates()) {
			for (var machine : machines) {
				sql.transaction(() -> loadMachineDefinition(sql, machine));
			}
		}
	}

	/**
	 * Add the machine definitions in the given configuration to the database.
	 *
	 * @param configuration
	 *            The configuration.
	 */
	public void loadMachineDefinitions(Configuration configuration) {
		try (var sql = new Updates()) {
			for (var machine : configuration.getMachines()) {
				sql.transaction(() -> loadMachineDefinition(sql, machine));
			}
		}
	}

	/**
	 * Add the machine definition to the database.
	 *
	 * @param machine
	 *            The machine definition.
	 */
	public void loadMachineDefinition(Machine machine) {
		try (var sql = new Updates()) {
			sql.transaction(() -> loadMachineDefinition(sql, machine));
		}
	}

	/**
	 * Possible exception when an insert fails.
	 *
	 * @author Donal Fellows
	 */
	public static class InsertFailedException extends RuntimeException {
		private static final long serialVersionUID = -4930512416142843777L;

		InsertFailedException(String table) {
			super("could not insert into " + table);
		}
	}

	/**
	 * Add a machine definition using the SQL update profile.
	 *
	 * @param sql
	 *            The SQL update profile (encapsulates both connection and
	 *            INSERTs).
	 * @param machine
	 *            The description of the machine to add.
	 * @return The ID of the created machine.
	 * @throws InsertFailedException
	 *             If the machine couldn't be created.
	 */
	private Integer loadMachineDefinition(Updates sql, Machine machine) {
		int machineId = makeMachine(sql, machine);
		var bmpIds = makeBMPs(sql, machine, machineId);
		var boardIds = makeBoards(sql, machine, machineId, bmpIds);
		makeLinks(sql, machine, boardIds);
		return machineId;
	}

	private int makeMachine(Updates sql, Machine machine)
			throws InsertFailedException {
		int machineId = sql.makeMachine.key(machine.getName(),
				machine.getWidth(), machine.getHeight(), machine.getDepth())
				.orElseThrow(() -> new InsertFailedException("machines"));
		// The above will blow up if the machine with that name exists
		machine.getTags().forEach(tag -> sql.makeTag.key(machineId, tag));
		return machineId;
	}

	private Map<BMPCoords, Integer> makeBMPs(Updates sql, Machine machine,
			int machineId) {
		var bmpIds = new HashMap<BMPCoords, Integer>();
		machine.bmpIPs.forEach((bmp, ip) -> sql.makeBMP
				.key(machineId, ip, bmp.getCabinet(), bmp.getFrame())
				.ifPresent(id -> bmpIds.put(bmp, id)));
		return bmpIds;
	}

	private Map<TriadCoords, Integer> makeBoards(Updates sql, Machine machine,
			int machineId, Map<BMPCoords, Integer> bmpIds) {
		var boardIds = new HashMap<TriadCoords, Integer>();
		int maxX = 0, maxY = 0;
		for (var triad : machine.boardLocations.keySet()) {
			var phys = machine.boardLocations.get(triad);
			int bmpID = bmpIds.get(phys.bmp());
			var addr = machine.spinnakerIPs.get(triad);
			var root = triad.chipLocation();
			log.debug("making {} board {}",
					machine.deadBoards.contains(triad) ? "dead" : "live",
					triad);
			sql.makeBoard
					.key(machineId, addr, bmpID, phys.b, triad.x, triad.y,
							triad.z, root.getX(), root.getY(),
							!machine.deadBoards.contains(triad))
					.ifPresent(id -> boardIds.put(triad, id));
			maxX = max(maxX, triad.x * TRIAD_CHIP_SIZE);
			maxY = max(maxY, triad.y * TRIAD_CHIP_SIZE);
		}
		/*
		 * Note that even in single-board setups, the max coordinates are as if
		 * there's a full triad present. It's just (in that case) that two of
		 * the boards of the triad aren't there.
		 */
		sql.setMaxCoords.call(maxX + TRIAD_CHIP_SIZE - 1,
				maxY + TRIAD_CHIP_SIZE - 1, machineId);
		var rootPhys = machine.boardLocations.get(new TriadCoords(0, 0, 0));
		for (var triad : machine.deadBoards) {
			// Fake with the machine root if no real coords available
			var phys = machine.boardLocations.getOrDefault(triad, rootPhys);
			int bmpID = bmpIds.get(phys.bmp());
			var root = triad.chipLocation();
			log.debug("making {} board {}", "dead", triad);
			sql.makeBoard
					.key(machineId, null, bmpID, null, triad.x, triad.y,
							triad.z, root.getX(), root.getY(), false)
					.ifPresent(id -> boardIds.put(triad, id));
		}
		return boardIds;
	}

	private void makeLinks(Updates sql, Machine machine,
			Map<TriadCoords, Integer> boardIds) {
		for (var here : boardIds.keySet()) {
			for (var d : Direction.values()) {
				var there = here.move(d, machine);
				if (boardIds.containsKey(there)) {
					makeLink(sql, machine, boardIds, here, d, there,
							d.opposite());
				}
			}
		}
	}

	private Optional<Integer> makeLink(Updates sql, Machine machine,
			Map<TriadCoords, Integer> boardIds, TriadCoords here, Direction d1,
			TriadCoords there, Direction d2) {
		var b1 = boardIds.get(here);
		var b2 = boardIds.get(there);
		if (isNull(b1) || isNull(b2)) {
			// No such board? Oh well
			return Optional.empty();
		}

		/*
		 * A link is dead if it is dead in either direction or if either board
		 * is dead.
		 */
		boolean dead = machine.deadBoards.contains(here)
				|| machine.hasDeadLinkAt(here, d1)
				|| machine.deadBoards.contains(there)
				|| machine.hasDeadLinkAt(there, d2);
		try {
			log.debug("making {}:{} <-{}-> {}:{}", here, d1, dead ? "/" : "-",
					there, d2);
			return sql.makeLink.key(b1, d1, b2, d2, !dead);
		} catch (DataAccessException e) {
			if (e.getMostSpecificCause() instanceof SQLiteException) {
				var exn = (SQLiteException) e.getMostSpecificCause();
				/*
				 * If the CHECK constraint says no, just ignore; we'll do the
				 * link from the other direction. This does mean we're doing too
				 * much work, but better to do too much and be reliable.
				 */
				if (exn.getResultCode() == SQLITE_CONSTRAINT_CHECK) {
					return Optional.empty();
				}
			}
			throw e;
		}
	}

	/** Operations for testing only. */
	@ForTestingOnly
	interface TestAPI {
		/**
		 * Add a machine definition using the SQL update profile.
		 *
		 * @param machine
		 *            The description of the machine to add.
		 * @return The ID of the created machine.
		 * @throws InsertFailedException
		 *             If the machine couldn't be created.
		 */
		Integer loadMachineDefinition(Machine machine);
	}

	/**
	 * @param c
	 *            How to talk to the DB.
	 * @return The test interface.
	 * @deprecated This interface is just for testing.
	 */
	@Deprecated
	@ForTestingOnly
	TestAPI getTestAPI(Connection c) {
		ForTestingOnly.Utils.checkForTestClassOnStack();
		return new TestAPI() {
			@Override
			public Integer loadMachineDefinition(Machine machine) {
				try (var updates = new Updates(c)) {
					return MachineDefinitionLoader.this
							.loadMachineDefinition(updates, machine);
				}
			}
		};
	}
}
