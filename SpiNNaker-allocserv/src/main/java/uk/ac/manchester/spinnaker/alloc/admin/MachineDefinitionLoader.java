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

import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.sqlite.SQLiteErrorCode.SQLITE_CONSTRAINT_CHECK;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_CHIP_SIZE;
import static uk.ac.manchester.spinnaker.alloc.Constants.TRIAD_DEPTH;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.copy;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Keep;
import com.google.errorprone.annotations.RestrictedApi;

import uk.ac.manchester.spinnaker.alloc.ForTestingOnly;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.alloc.model.Direction;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.machine.board.PhysicalCoords;
import uk.ac.manchester.spinnaker.machine.board.TriadCoords;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadHeight;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadWidth;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;
import uk.ac.manchester.spinnaker.utils.validation.TCPPort;

/**
 * Loads definitions of machines from JSON, as generated by {@code py2json}.
 *
 * @author Donal Fellows
 */
@Service
public class MachineDefinitionLoader extends DatabaseAwareBean {
	private static final Logger log = getLogger(MachineDefinitionLoader.class);

	/**
	 * Enumeration of links from a SpiNNaker chip, as used in the old spalloc.
	 * <p>
	 * Note that the numbers chosen have two useful properties:
	 *
	 * <ul>
	 * <li>The integer values assigned are chosen to match the numbers used to
	 * identify the links in the low-level software API and hardware registers.
	 * <li>The links are ordered consecutively in anticlockwise order meaning
	 * the opposite link is {@code (link+3)%6}.
	 * </ul>
	 * Note that the new Spalloc uses a different notation for link directions!
	 *
	 * @see Direction
	 * @author Donal Fellows
	 */
	public enum Link {
		/** East. */
		east(Direction.SE),
		/** North-East. */
		northEast(Direction.E),
		/** North. */
		north(Direction.N),
		/** West. */
		west(Direction.NW),
		/** South-West. */
		southWest(Direction.W),
		/** South. */
		south(Direction.S);

		private static final Map<Direction, Link> MAP =
				makeEnumBackingMap(values(), v -> v.d);

		private final Direction d;

		Link(Direction d) {
			this.d = d;
		}

		static Link of(Direction direction) {
			return MAP.get(direction);
		}
	}

	/**
	 * A machine description. JSON-serializable.
	 *
	 * @author Donal Fellows
	 */
	@JsonDeserialize(builder = Machine.Builder.class)
	public static final class Machine {
		@NotBlank(message = "machines must have real names")
		private String name;

		private Set<@NotBlank String> tags;

		@ValidTriadWidth
		private int width;

		@ValidTriadHeight
		private int height;

		private Set<@Valid TriadCoords> deadBoards;

		private Map<@Valid TriadCoords, @NotNull EnumSet<Link>> deadLinks;

		private Map<@Valid TriadCoords, @Valid PhysicalCoords> boardLocations;

		private Map<@Valid BMPCoords, @IPAddress String> bmpIPs;

		private Map<@Valid TriadCoords, @IPAddress String> spinnakerIPs;

		/** @return The name of the machine. */
		public String getName() {
			return name;
		}

		/** @return The tags of the machine. */
		public Set<String> getTags() {
			return tags;
		}

		/** @return The width of the machine, in triads. */
		public int getWidth() {
			return width;
		}

		/** @return The height of the machine, in triads. */
		public int getHeight() {
			return height;
		}

		/** @return The depth of the machine, the number of boards per triad. */
		public int getDepth() {
			return boardLocations.size() == 1 ? 1 : TRIAD_DEPTH;
		}

		/** @return The dead boards of the machine. */
		public Set<TriadCoords> getDeadBoards() {
			return deadBoards;
		}

		/**
		 * @return The extra dead links of the machine. Doesn't include links to
		 *         dead boards.
		 */
		public Map<TriadCoords, EnumSet<Link>> getDeadLinks() {
			return deadLinks;
		}

		/** @return The logical-to-physical board location map. */
		public Map<TriadCoords, PhysicalCoords> getBoardLocations() {
			return boardLocations;
		}

		/** @return The IP addresses of the BMPs. */
		public Map<BMPCoords, String> getBmpIPs() {
			return bmpIPs;
		}

		/** @return The IP addresses of the boards. */
		public Map<TriadCoords, String> getSpinnakerIPs() {
			return spinnakerIPs;
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

		@Keep
		@JsonIgnore
		@AssertFalse(message = "machine name must not contain braces or spaces")
		private boolean isBadMachineName() {
			return badName(name);
		}

		@Keep
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

		@Keep
		@JsonIgnore
		@AssertTrue(message = "all boards must have sane logical coordinates")
		private boolean isCoordinateSane() {
			return boardLocations.keySet().stream().allMatch(loc -> (loc.x >= 0)
					&& (loc.x < width) && (loc.y >= 0) && (loc.y < height));
		}

		@Keep
		@JsonIgnore
		@AssertTrue(message = "all boards must have addresses")
		private boolean isNetworkSane() {
			return spinnakerIPs.size() == boardLocations.size();
		}

		@Keep
		@JsonIgnore
		@AssertTrue(message = "all boards must have BMPs")
		private boolean isBMPSane() {
			return boardLocations.values().stream()
					.map(PhysicalCoords::getBmpCoords)
					.allMatch(bmpIPs::containsKey);
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

		/**
		 * Applies a wraparound rule in a particular direction, turning
		 * coordinate space into something of a modular field.
		 *
		 * @param value
		 *            The value to wrap.
		 * @param limit
		 *            The upper limit. (Lower limits are always zero.)
		 * @return The potentially wrapped value.
		 */
		private static int limit(int value, int limit) {
			if (value < 0) {
				return value + limit;
			} else if (value >= limit) {
				return value - limit;
			} else {
				return value;
			}
		}

		/**
		 * Get the triad coordinate that you arrive at when you move from a
		 * given location in the indicated direction. This ignores dead links
		 * and dead boards, but respects wraparounds.
		 *
		 * @param here
		 *            Where to move from
		 * @param direction
		 *            Which way to move
		 * @return The new location
		 */
		private TriadCoords move(TriadCoords here, Direction direction) {
			var di = DirInfo.get(here.z, direction);
			return new TriadCoords(limit(here.x + di.dx, width),
					limit(here.y + di.dy, height), here.z + di.dz);
		}

		@JsonPOJOBuilder
		static class Builder {
			private String name;

			private Set<String> tags = Set.of();

			private int width;

			private int height;

			private Set<TriadCoords> deadBoards = Set.of();

			private Map<TriadCoords, EnumSet<Link>> deadLinks = Map.of();

			private Map<TriadCoords, PhysicalCoords> boardLocations = Map.of();

			private Map<BMPCoords, String> bmpAddrs = Map.of();

			private Map<TriadCoords, String> spinnakerAddrs = Map.of();

			@CanIgnoreReturnValue
			public Builder withName(String name) {
				this.name = name;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder withTags(Set<String> tags) {
				this.tags = tags;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder withWidth(int width) {
				this.width = width;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder withHeight(int height) {
				this.height = height;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder withDeadBoards(Set<TriadCoords> deadBoards) {
				this.deadBoards = deadBoards;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder
					withDeadLinks(Map<TriadCoords, EnumSet<Link>> deadLinks) {
				this.deadLinks = deadLinks;
				return this;
			}

			@CanIgnoreReturnValue
			public Builder withBoardLocations(
					Map<TriadCoords, PhysicalCoords> boardLocations) {
				this.boardLocations = boardLocations;
				return this;
			}

			@CanIgnoreReturnValue
			@JsonProperty("bmp-ips")
			public Builder withBmpAddrs(Map<BMPCoords, String> bmpAddrs) {
				this.bmpAddrs = bmpAddrs;
				return this;
			}

			@CanIgnoreReturnValue
			@JsonProperty("spinnaker-ips")
			public Builder withSpinnakerAddrs(
					Map<TriadCoords, String> spinnakerAddrs) {
				this.spinnakerAddrs = spinnakerAddrs;
				return this;
			}

			public Machine build() {
				var m = new Machine();
				m.name = name;
				m.tags = copy(tags);
				m.width = width;
				m.height = height;
				m.deadBoards = copy(deadBoards);
				m.deadLinks = copy(deadLinks);
				m.boardLocations = copy(boardLocations);
				m.bmpIPs = copy(bmpAddrs);
				m.spinnakerIPs = copy(spinnakerAddrs);
				return m;
			}
		}
	}

	/**
	 * A configuration description. JSON-deserializable (the only supported
	 * mechanism for generating an instance). Largely ignored as it represents
	 * configuration settings that we handle elsewhere. However, the
	 * {@code machines} property <em>is</em> interesting.
	 *
	 * @author Donal Fellows
	 */
	public static final class Configuration {
		private @NotNull List<@Valid Machine> machines;

		@TCPPort
		private int port;

		@IPAddress
		private String ip;

		@Positive
		private double timeoutCheckInterval;

		@Positive
		private int maxRetiredJobs;

		@Positive
		private int secondsBeforeFree;

		/** @return The machines to manage. */
		public List<Machine> getMachines() {
			return machines;
		}

		void setMachines(List<Machine> machines) {
			this.machines = copy(machines);
		}

		/** @return The port for the service to listen on. (Ignored) */
		public int getPort() {
			return port;
		}

		void setPort(int port) {
			this.port = port;
		}

		/**
		 * @return The host address for the service to listen on. Empty = all
		 *         interfaces. (Ignored)
		 */
		public String getIp() {
			return ip;
		}

		void setIp(String ip) {
			this.ip = ip;
		}

		/** @return How often (in seconds) to check for timeouts. (Ignored) */
		public double getTimeoutCheckInterval() {
			return timeoutCheckInterval;
		}

		void setTimeoutCheckInterval(double timeoutCheckInterval) {
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

		void setSecondsBeforeFree(int secondsBeforeFree) {
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
	 * @return The machines from that file. Not {@code null}.
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
				.key(machineId, ip, bmp.cabinet, bmp.frame)
				.ifPresent(id -> bmpIds.put(bmp, id)));
		return bmpIds;
	}

	private Map<TriadCoords, Integer> makeBoards(Updates sql, Machine machine,
			int machineId, Map<BMPCoords, Integer> bmpIds) {
		var boardIds = new HashMap<TriadCoords, Integer>();
		int maxX = 0, maxY = 0;
		for (var triad : machine.boardLocations.keySet()) {
			var phys = machine.boardLocations.get(triad);
			int bmpID = bmpIds.get(phys.getBmpCoords());
			var addr = machine.spinnakerIPs.get(triad);
			var root = triad.asChipLocation();
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
			int bmpID = bmpIds.get(phys.getBmpCoords());
			var root = triad.asChipLocation();
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
				var there = machine.move(here, d);
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
	@RestrictedApi(explanation = "just for testing", link = "index.html",
			allowedOnPath = ".*/src/test/java/.*")
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
