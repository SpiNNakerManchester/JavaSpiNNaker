/*
 * Copyright (c) 2022 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.EnumSet.noneOf;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.alloc.model.Utils.chip;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.toEnumSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import uk.ac.manchester.spinnaker.alloc.db.DatabaseAwareBean;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Connection;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Query;
import uk.ac.manchester.spinnaker.alloc.db.DatabaseEngine.Update;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

/**
 * Read a blacklist from a string, a definition file or the database. Note that
 * the code does not examine the filename, which is often used to determine what
 * board the blacklist applies to; that is left as a problem for the caller to
 * handle. That information <em>is</em> required when talking to the DB.
 *
 * @author Donal Fellows
 */
@Component
public class BlacklistIO extends DatabaseAwareBean {
	private static final SpiNNakerTriadGeometry GEOM = getSpinn5Geometry();

	/**
	 * Read a blacklist from the database.
	 *
	 * @param boardId
	 *            The ID of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *             If database access fails.
	 */
	public Optional<Blacklist> readBlacklistFromDB(int boardId) {
		return executeRead(conn -> readBlacklistFromDB(conn, boardId));
	}

	/**
	 * Read a blacklist from the database.
	 *
	 * @param conn
	 *            The database connection.
	 * @param boardId
	 *            The ID of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *             If database access fails.
	 * @deprecated Call via {@link #readBlacklistFromDB(int)} if not in test
	 *             code.
	 */
	@Deprecated
	final Optional<Blacklist> readBlacklistFromDB(Connection conn,
			int boardId) {
		try (Query blChips = conn.query(GET_BLACKLISTED_CHIPS);
				Query blCores = conn.query(GET_BLACKLISTED_CORES);
				Query blLinks = conn.query(GET_BLACKLISTED_LINKS)) {
			Set<ChipLocation> blacklistedChips = blChips.call(boardId)
					.map(chip("x", "y")).toSet();
			Map<ChipLocation, Set<Integer>> blacklistedCores = blCores
					.call(boardId).toCollectingMap(HashSet::new,
							chip("x", "y"), integer("p"));
			Map<ChipLocation, Set<Direction>> blacklistedLinks = blLinks
					.call(boardId).toCollectingMap(
							() -> noneOf(Direction.class), chip("x", "y"),
							enumerate("direction", Direction.class));

			if (blacklistedChips.isEmpty() && blacklistedCores.isEmpty()
					&& blacklistedLinks.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(new Blacklist(blacklistedChips, blacklistedCores,
					blacklistedLinks));
		}
	}

	/**
	 * Save a blacklist in the database.
	 *
	 * @param boardId
	 *            What board is this a blacklist for?
	 * @param blacklist
	 *            The blacklist to save.
	 */
	public void writeBlacklistToDB(Integer boardId, Blacklist blacklist) {
		requireNonNull(blacklist);
		execute(conn -> {
			writeBlacklistToDB(conn, boardId, blacklist);
			return this; // dummy
		});
	}

	/**
	 * Save a blacklist in the database.
	 *
	 * @param conn
	 *            Which database?
	 * @param boardId
	 *            What board is this a blacklist for?
	 * @param blacklist
	 *            The blacklist to save.
	 * @deprecated Call via {@link #writeBlacklistToDB(Integer, Blacklist)} if
	 *             not in test code.
	 */
	@Deprecated
	void writeBlacklistToDB(Connection conn, Integer boardId,
			Blacklist blacklist) {
		try (Update clearChips = conn.update(CLEAR_BLACKLISTED_CHIPS);
				Update clearCores = conn.update(CLEAR_BLACKLISTED_CORES);
				Update clearLinks = conn.update(CLEAR_BLACKLISTED_LINKS);
				Update addChip = conn.update(ADD_BLACKLISTED_CHIP);
				Update addCore = conn.update(ADD_BLACKLISTED_CORE);
				Update addLink = conn.update(ADD_BLACKLISTED_LINK)) {
			// TODO Keep the old blacklist data where it is the same as before
			// Remove the old information
			clearChips.call(boardId);
			clearCores.call(boardId);
			clearLinks.call(boardId);
			// Write the new information
			blacklist.getChips().forEach(chip -> {
				addChip.call(boardId, chip.getX(), chip.getY());
			});
			blacklist.getCores().forEach((chip, cores) -> cores.forEach(c -> {
				addCore.call(boardId, chip.getX(), chip.getY(), c);
			}));
			blacklist.getLinks().forEach((chip, links) -> links.forEach(l -> {
				addLink.call(boardId, chip.getX(), chip.getY(), l);
			}));
		}
	}

	/**
	 * Read a blacklist from a file.
	 *
	 * @param file
	 *            The file to read from.
	 * @return The parsed blacklist.
	 * @throws IOException
	 *             If the file can't be read from.
	 * @throws IllegalArgumentException
	 *             If the file is badly formatted.
	 */
	public Blacklist readBlacklistFile(File file) throws IOException {
		requireNonNull(file);
		try (FileReader r = new FileReader(file);
				BufferedReader br = new BufferedReader(r)) {
			return readBlacklist(br);
		}
	}

	/**
	 * Read a blacklist from a string.
	 *
	 * @param blacklistText
	 *            The string to parse.
	 * @return The parsed blacklist.
	 * @throws IOException
	 *             If the string can't be read from. (Not expected.)
	 * @throws IllegalArgumentException
	 *             If the string is badly formatted.
	 */
	public Blacklist parseBlacklist(String blacklistText) throws IOException {
		requireNonNull(blacklistText);
		try (BufferedReader br =
				new BufferedReader(new StringReader(blacklistText))) {
			return readBlacklist(br);
		}
	}

	// REs from Perl code to read blacklist files

	private static final Pattern CHIP_PATTERN = compile(
			"chip\\s+(?<x>[0-7])\\s+(?<y>[0-7])\\s+(?<rest>.+)$");

	private static final Pattern CORE_PATTERN = compile(
			"core\\s+(?<cores>\\S+)\\s*");

	private static final Pattern LINK_PATTERN = compile(
			"link\\s+(?<links>\\S+)\\s*");

	private static final Pattern DEAD_PATTERN = compile("dead\\s*");

	private static String deleteMatched(Matcher m) {
		// Java 8 uses StringBuffer for this; WHYWHYWHY?!
		StringBuffer sb = new StringBuffer();
		m.appendReplacement(sb, "").appendTail(sb);
		return sb.toString();
	}

	private static Set<Integer> parseCommaSeparatedSet(String str) {
		return stream(str.split(",")).map(Integer::parseInt).collect(toSet());
	}

	private static <T extends Enum<T>> Set<T> parseCommaSeparatedSet(
			String str, Function<Integer, T> fun, Class<T> cls) {
		return stream(str.split(",")).map(Integer::parseInt).map(fun)
				.collect(toEnumSet(cls));
	}

	/**
	 * Read a blacklist from an input reader.
	 *
	 * @param r
	 *           The reader to read from.
	 * @return The parsed blacklist.
	 * @throws IOException
	 *           If the reader can't be read from.
	 * @throws IllegalArgumentException
	 *           If the reader's content is badly formatted.
	 */
	public Blacklist readBlacklist(BufferedReader r) throws IOException {
		try {
			Map<ChipLocation, Set<Integer>> deadCores = new HashMap<>();
			Map<ChipLocation, Set<Direction>> deadLinks = new HashMap<>();
			Set<ChipLocation> deadChips = new HashSet<>();

			r.lines().map(String::trim)
					// Remove blank and comment lines
					.filter(s -> !s.isEmpty() && !s.startsWith("#"))
					// Parse the remaining lines
					.forEach(line -> parseLine(line, deadChips, deadCores,
							deadLinks));

			return new Blacklist(deadChips, deadCores, deadLinks);
		} catch (UncheckedIOException e) {
			// Unhide the exception (see the doc for BufferedReader.lines())
			throw e.getCause();
		}
	}

	/**
	 * Parse one non-empty non-comment line of a blacklist file.
	 *
	 * @param line
	 *            The line's contents.
	 * @param deadChips
	 *            Where to accumulate dead chips.
	 * @param deadCores
	 *            Where to accumulate dead cores.
	 * @param deadLinks
	 *            Where to accumulate dead links.
	 */
	private void parseLine(String line, Set<ChipLocation> deadChips,
			Map<ChipLocation, Set<Integer>> deadCores,
			Map<ChipLocation, Set<Direction>> deadLinks) {
		Matcher m = CHIP_PATTERN.matcher(line);
		if (!m.matches()) {
			throw new IllegalArgumentException("bad line: " + line);
		}
		int x = parseInt(m.group("x"));
		int y = parseInt(m.group("y"));
		ChipLocation chip = new ChipLocation(x, y);
		if (!GEOM.singleBoard().contains(chip)) {
			throw new IllegalArgumentException("bad chip coords: " + line);
		}
		String rest = m.group("rest");

		ChipLocation dead = null;
		Set<Integer> cores = null;
		Set<Direction> links = null;

		// Look for patterns at start of line while we can
		while (true) {
			m = CORE_PATTERN.matcher(rest);
			if (m.find() && cores == null) {
				cores = parseCommaSeparatedSet(m.group("cores"));
				cores.forEach(c -> {
					if (c < 0 || c >= PROCESSORS_PER_CHIP) {
						throw new IllegalArgumentException(
								"bad core number: " + line);
					}
				});
				rest = deleteMatched(m);
				continue;
			}

			m = LINK_PATTERN.matcher(rest);
			if (m.find() && links == null) {
				links = parseCommaSeparatedSet(m.group("links"),
						Direction::byId, Direction.class);
				rest = deleteMatched(m);
				continue;
			}

			m = DEAD_PATTERN.matcher(rest);
			if (m.find() && dead == null) {
				dead = chip;
				rest = deleteMatched(m);
				continue;
			}

			// All done, or error
			if (!rest.isEmpty()) {
				// Bad line
				throw new IllegalArgumentException("bad line: " + line);
			}
			break;
		}

		if (dead != null) {
			deadChips.add(dead);
		} else {
			if (cores != null && !cores.isEmpty()) {
				deadCores.put(chip, cores);
			}
			if (links != null && !links.isEmpty()) {
				deadLinks.put(chip, links);
			}
		}
	}

	/**
	 * Write a blacklist to a stream in a human-readable format. This is the
	 * format understood by {@link #readBlacklist(BufferedReader)}.
	 * <p>
	 * Note that the result may omit information in the original blacklist, but
	 * only if that would also be ignored by the reader side.
	 *
	 * @param blacklist
	 *            The blacklist to write
	 * @param w
	 *            Where to write the blacklist
	 * @throws IOException
	 *             If the blacklist couldn't be written.
	 */
	public void writeBlacklist(Blacklist blacklist, BufferedWriter w)
			throws IOException {
		requireNonNull(blacklist);
		Set<ChipLocation> chips = blacklist.getChips();
		Map<ChipLocation, Set<Integer>> cores = blacklist.getCores();
		Map<ChipLocation, Set<Direction>> links = blacklist.getLinks();
		try (PrintWriter out = new PrintWriter(w)) {
			// Don't use println(); not exactly portable on Windows
			for (ChipLocation chip : GEOM.singleBoard()) {
				if (!blacklist.isChipMentioned(chip)) {
					continue;
				}
				out.format("chip %d %d", chip.getX(), chip.getY());
				if (chips.contains(chip)) {
					out.print(" dead\n");
				} else {
					if (cores.containsKey(chip)) {
						out.print(" core ");
						String sep = "";
						for (Integer id : cores.get(chip)) {
							out.append(sep).append(id.toString());
							sep = ",";
						}
					}
					if (links.containsKey(chip)) {
						out.print(" link ");
						String sep = "";
						for (Direction d : links.get(chip)) {
							out.append(sep).append(Integer.toString(d.id));
							sep = ",";
						}
					}
					out.print("\n");
				}
			}
			if (out.checkError()) {
				throw new IOException("failed to write blacklist");
			}
		}
	}

	/**
	 * Convert a blacklist to a string in a human-readable format. This is the
	 * format understood by {@link #parseBlacklist(String)}.
	 * <p>
	 * Note that the result may omit information in the original blacklist, but
	 * only if that would also be ignored by the string parser.
	 *
	 * @param blacklist
	 *            The blacklist to convert
	 * @return The string form of the blacklist.
	 * @throws RuntimeException If something goes wrong. Not expected!
	 */
	public String toString(Blacklist blacklist) {
		requireNonNull(blacklist);
		try (StringWriter sw = new StringWriter();
				BufferedWriter bw = new BufferedWriter(sw)) {
			writeBlacklist(blacklist, bw);
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected exception while writing blacklist to string",
					e);
		}
	}
}
