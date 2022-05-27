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
package uk.ac.manchester.spinnaker.messages.bmp;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.noneOf;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.MachineDefaults;

/**
 * A blacklist read off a board. Note that all chip coordinates are
 * board-relative.
 *
 * @author Donal Fellows
 */
public class Blacklist {
	private static final int NUM_CORES = MachineDefaults.MAX_NUM_CORES;

	private static final int NUM_LINKS = MachineDefaults.MAX_LINKS_PER_ROUTER;

	private static final int COORD_BITS = 3;

	private static final int COORD_MASK = 0x7;

	private static final int CORE_MASK = (1 << NUM_CORES) - 1;

	private static final int LINK_MASK = (1 << NUM_LINKS) - 1;

	private Set<ChipLocation> chips = new HashSet<>();

	private Map<ChipLocation, Set<Integer>> cores = new HashMap<>();

	private Map<ChipLocation, Set<Direction>> links = new HashMap<>();

	/**
	 * Create a blacklist from raw data.
	 *
	 * @param buffer
	 *            The raw data to parse.
	 */
	public Blacklist(ByteBuffer buffer) {
		ByteBuffer buf = buffer.duplicate();
		buf.order(LITTLE_ENDIAN);
		IntBuffer blacklistEntries = buf.asIntBuffer();
		int len = blacklistEntries.get();
		Set<ChipLocation> done = new HashSet<>();

		for (int i = 0; i < len; i++) {
			int entry = blacklistEntries.get();

			// get board coordinates
			int bx = (entry >> (NUM_CORES + NUM_LINKS + COORD_BITS))
					& COORD_MASK;
			int by = (entry >> (NUM_CORES + NUM_LINKS)) & COORD_MASK;
			ChipLocation b = new ChipLocation(bx, by);

			// check for repeated coordinates
			if (done.contains(b)) {
				// TODO this should be a log/warning
				System.out.println("duplicate chip " + bx + "," + by);
			}
			done.add(b);

			// check for blacklisted chips
			int mcl = entry & CORE_MASK;
			if (mcl == CORE_MASK) {
				chips.add(b);
			} else if (mcl != 0) {
				// check for blacklisted cores
				cores.put(b,
						range(0, NUM_CORES).filter(c -> (mcl & (1 << c)) != 0)
								.mapToObj(Integer::valueOf).collect(toSet()));
			}

			// check for blacklisted links
			int mll = (entry >> NUM_CORES) & LINK_MASK;
			if (mll != 0) {
				links.put(b, range(0, NUM_LINKS)
						.filter(c -> (mll & (1 << c)) != 0)
						.mapToObj(Direction::byId)
						.collect(toCollection(() -> noneOf(Direction.class))));
			}
		}
	}

	/**
	 * @return The chips on the board that are blacklisted. A chip being
	 *         blacklisted means that its links will also be blacklisted.
	 */
	public Set<ChipLocation> getChips() {
		return unmodifiableSet(chips);
	}

	/**
	 * @return The cores on the board that are blacklisted where the whole
	 *         chip is not blacklisted.
	 */
	public Map<ChipLocation, Set<Integer>> getCores() {
		return unmodifiableMap(cores);
	}

	/**
	 * @return The links on the board that are blacklisted.
	 */
	public Map<ChipLocation, Set<Direction>> getLinks() {
		return unmodifiableMap(links);
	}
}
