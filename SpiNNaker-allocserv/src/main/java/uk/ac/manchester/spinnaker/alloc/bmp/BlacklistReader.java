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
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;
import static uk.ac.manchester.spinnaker.alloc.db.Row.enumerate;
import static uk.ac.manchester.spinnaker.alloc.db.Row.integer;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.toEnumSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import uk.ac.manchester.spinnaker.alloc.db.Row;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

/**
 * Read a blacklist from a definition file or the database. Note that the code
 * does not examine the filename, which is often used to determine what board
 * the blacklist applies to; that is left as a problem for the caller to handle.
 *
 * @author Donal Fellows
 */
@Component
public class BlacklistReader extends DatabaseAwareBean {
	/**
	 * Read a blacklist from the database.
	 *
	 * @param serialNumber
	 *            The physical serial number of the board.
	 * @return The blacklist, if one is defined.
	 * @throws DataAccessException
	 *            If database access fails.
	 */
	public Optional<Blacklist> readBlacklistFromDB(String serialNumber) {
		return execute(conn -> readBlacklistFromDB(conn, serialNumber));
	}

	private static ChipLocation chip(Row row) {
		return new ChipLocation(row.getInt("x"), row.getInt("y"));
	}

	private Optional<Blacklist> readBlacklistFromDB(Connection conn,
			String serialNumber) {
		try (Query blChips = conn.query(GET_BLACKLISTED_CHIPS);
				Query blCores = conn.query(GET_BLACKLISTED_CORES);
				Query blLinks = conn.query(GET_BLACKLISTED_LINKS)) {
			Set<ChipLocation> blacklistedChips = blChips.call(serialNumber)
					.map(BlacklistReader::chip).toSet();
			Map<ChipLocation, Set<Integer>> blacklistedCores = blCores
					.call(serialNumber).toCollectingMap(HashSet::new,
							BlacklistReader::chip, integer("p"));
			Map<ChipLocation, Set<Direction>> blacklistedLinks = blLinks
					.call(serialNumber)
					.toCollectingMap(() -> noneOf(Direction.class),
							BlacklistReader::chip,
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
	 * Read a blacklist from a file.
	 *
	 * @param file
	 *           The file to read from.
	 * @return The parsed blacklist.
	 * @throws IOException
	 *           If the file can't be read from.
	 * @throws IllegalArgumentException
	 *           If the file is badly formatted.
	 */
	public Blacklist readBlacklistFile(File file) throws IOException {
		try (FileReader r = new FileReader(file);
				BufferedReader br = new BufferedReader(r)) {
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
		Map<ChipLocation, Set<Integer>> deadCores = new HashMap<>();
		Map<ChipLocation, Set<Direction>> deadLinks = new HashMap<>();
		Set<ChipLocation> deadChips = new HashSet<>();

		String line;
		while ((line = r.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				// Skip blanks and comments
				continue;
			}
			parseLine(line, deadChips, deadCores, deadLinks);
		}

		return new Blacklist(deadChips, deadCores, deadLinks);
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
			return;
		}
		int x = parseInt(m.group("x"));
		int y = parseInt(m.group("y"));
		ChipLocation chip = new ChipLocation(x, y);
		String rest = m.group("rest");

		Set<Integer> cores = null;
		m = CORE_PATTERN.matcher(rest);
		if (m.find()) {
			cores = parseCommaSeparatedSet(m.group("cores"));
			rest = deleteMatched(m);
		}

		Set<Direction> links = null;
		m = LINK_PATTERN.matcher(rest);
		if (m.find()) {
			links = parseCommaSeparatedSet(m.group("links"), Direction::byId,
					Direction.class);
			rest = deleteMatched(m);
		}

		boolean dead = false;
		m = DEAD_PATTERN.matcher(rest);
		if (m.find()) {
			dead = true;
			rest = deleteMatched(m);
		}

		if (!rest.isEmpty()) {
			// Bad line
			throw new IllegalArgumentException("bad line: " + line);
		}

		if (dead) {
			deadChips.add(chip);
		} else {
			if (cores != null && !cores.isEmpty()) {
				deadCores.put(chip, cores);
			}
			if (links != null && !links.isEmpty()) {
				deadLinks.put(chip, links);
			}
		}
	}
}
