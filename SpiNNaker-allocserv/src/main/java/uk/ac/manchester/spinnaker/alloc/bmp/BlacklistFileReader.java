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
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

public class BlacklistFileReader {
	public Blacklist readBlacklistFile(File f) throws IOException {
		try (FileReader r = new FileReader(f);
				BufferedReader br = new BufferedReader(r)) {
			return readBlacklist(br);
		}
	}

	private static Pattern chipPattern = compile(
			"chip\\s+(?<x>[0-7])\\s+(?<y>[0-7])\\s+(?<rest>.+)$");

	private static Pattern corePattern = compile("core\\s+(?<cores>\\S+)\\s*");

	private static Pattern linkPattern = compile("link\\s+(?<links>\\S+)\\s*");

	private static Pattern deadPattern = compile("dead\\s*");

	private static String deleteMatched(Matcher m) {
		// 8 uses StringBuffer for this; WHYWHYWHY?!
		StringBuffer sb = new StringBuffer();
		m.appendReplacement(sb, "").appendTail(sb);
		return sb.toString();
	}

	private static Set<Integer> parseCommaSeparatedSet(String str) {
		return Arrays.stream(str.split(",")).map(Integer::parseInt)
				.collect(toSet());
	}

	private static <T> Set<T> parseCommaSeparatedSet(String str,
			Function<Integer, T> fun) {
		return Arrays.stream(str.split(",")).map(Integer::parseInt).map(fun)
				.collect(toSet());
	}

	public Blacklist readBlacklist(BufferedReader r) throws IOException {
		String line;
		Map<ChipLocation, Set<Integer>> deadCores = new HashMap<>();
		Map<ChipLocation, Set<Direction>> deadLinks = new HashMap<>();
		Set<ChipLocation> deadChips = new HashSet<>();
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

	private void parseLine(String line, Set<ChipLocation> deadChips,
			Map<ChipLocation, Set<Integer>> deadCores,
			Map<ChipLocation, Set<Direction>> deadLinks) {
		Matcher m = chipPattern.matcher(line);
		if (!m.matches()) {
			return;
		}
		int x = parseInt(m.group("x"));
		int y = parseInt(m.group("y"));
		ChipLocation chip = new ChipLocation(x, y);
		String rest = m.group("rest");

		Set<Integer> cores = null;
		m = corePattern.matcher(rest);
		if (m.find()) {
			cores = parseCommaSeparatedSet(m.group("cores"));
			rest = deleteMatched(m);
		}

		Set<Direction> links = null;
		m = linkPattern.matcher(rest);
		if (m.find()) {
			links = parseCommaSeparatedSet(m.group("links"), Direction::byId);
			rest = deleteMatched(m);
		}

		boolean dead = false;
		m = deadPattern.matcher(rest);
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
