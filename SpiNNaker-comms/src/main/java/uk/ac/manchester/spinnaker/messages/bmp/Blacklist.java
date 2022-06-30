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

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_LINKS_PER_ROUTER;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_X_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_Y_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.OR;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.toEnumSet;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 * A blacklist read off a board. Note that all chip coordinates are
 * board-relative and all processor IDs are physical; the boot process applies
 * blacklists before inter-board links are brought up and before the
 * virtual-to-physical core mapping is established.
 *
 * @author Donal Fellows
 */
public final class Blacklist implements Serializable {
	private static final long serialVersionUID = -7759940789892168209L;

	private static final Logger log = getLogger(Blacklist.class);

	private static final int SPINN5_CHIPS_PER_BOARD = 48;

	private static final int COORD_BITS = 3;

	private static final int COORD_MASK = (1 << COORD_BITS) - 1;

	private static final int CORE_MASK = (1 << MAX_NUM_CORES) - 1;

	private static final int LINK_MASK = (1 << MAX_LINKS_PER_ROUTER) - 1;

	private static final int PAYLOAD_BITS = MAX_NUM_CORES
			+ MAX_LINKS_PER_ROUTER;

	private ByteBuffer rawData;

	private Set<ChipLocation> chips = new HashSet<>();

	private Map<ChipLocation, Set<Integer>> cores = new HashMap<>();

	private Map<ChipLocation, Set<Direction>> links = new HashMap<>();

	/**
	 * Create a blacklist from raw data.
	 *
	 * @param buffer
	 *           The raw data to parse.
	 */
	public Blacklist(ByteBuffer buffer) {
		ByteBuffer buf = buffer.duplicate();
		buf.order(LITTLE_ENDIAN);
		rawData = buf.asReadOnlyBuffer().order(LITTLE_ENDIAN);
		decodeBlacklist(buf);
	}

	/**
	 * Create a blacklist from parsed data.
	 *
	 * @param deadChips
	 *           The set of chips that are dead.
	 * @param deadCores
	 *           The set of physical core IDs that are dead on live chips.
	 * @param deadLinks
	 *           The set of link directions that are dead on live chips.
	 */
	public Blacklist(Set<ChipLocation> deadChips,
			Map<ChipLocation, Set<Integer>> deadCores,
			Map<ChipLocation, Set<Direction>> deadLinks) {
		chips = deadChips;
		cores = deadCores;
		links = deadLinks;
		rawData = encodeBlacklist().asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}

	private ByteBuffer encodeBlacklist() {
		ByteBuffer buf = allocate((SPINN5_CHIPS_PER_BOARD + 1) * WORD_SIZE);
		buf.order(LITTLE_ENDIAN);
		buf.putInt(0); // Size; filled in later
		int count = 0;
		for (int x = 0; x < SIZE_X_OF_ONE_BOARD; x++) {
			for (int y = 0; y < SIZE_Y_OF_ONE_BOARD; y++) {
				ChipLocation chip = new ChipLocation(x, y);
				int loc = (x << COORD_BITS) | y;
				int value = 0;
				if (chips.contains(chip)) {
					value = CORE_MASK;
				} else {
					if (cores.containsKey(chip)) {
						value |= cores.get(chip).stream()
								.mapToInt(core -> 1 << core) //
								.reduce(0, OR) & CORE_MASK;
					}
					if (links.containsKey(chip)) {
						value |= links.get(chip).stream()
								.mapToInt(linkDir -> 1 << linkDir.id)
								.reduce(0, OR) << MAX_NUM_CORES;
					}
				}
				if (value != 0) {
					buf.putInt(value | loc << PAYLOAD_BITS);
					count++;
				}
			}
		}
		buf.flip();
		buf.putInt(0, count); // Fill in the size now we know it
		return buf;
	}

	private void decodeBlacklist(ByteBuffer buf) {
		IntBuffer entries = buf.asIntBuffer();
		int len = entries.get();
		Set<ChipLocation> done = new HashSet<>();

		for (int i = 0; i < len; i++) {
			int entry = entries.get();

			// get board coordinates
			int bx = (entry >> (PAYLOAD_BITS + COORD_BITS)) & COORD_MASK;
			int by = (entry >> PAYLOAD_BITS) & COORD_MASK;
			ChipLocation b = new ChipLocation(bx, by);

			// check for repeated coordinates
			if (done.contains(b)) {
				log.warn("duplicate chip in blacklist file: {},{}", bx, by);
			}
			done.add(b);

			/*
			 * Check for blacklisted chips; those are the ones where all cores
			 * are blacklisted so no monitor is safe to bring up.
			 */
			int mcl = entry & CORE_MASK;
			if (mcl == CORE_MASK) {
				chips.add(b);
			} else if (mcl != 0) {
				// check for blacklisted cores
				cores.put(b,
						range(0, MAX_NUM_CORES)
								.filter(c -> (mcl & (1 << c)) != 0)
								.mapToObj(Integer::valueOf).collect(toSet()));
				// check for blacklisted links
				int mll = (entry >> MAX_NUM_CORES) & LINK_MASK;
				if (mll != 0) {
					links.put(b, range(0, MAX_LINKS_PER_ROUTER)
							.filter(c -> (mll & (1 << c)) != 0)
							.mapToObj(Direction::byId)
							.collect(toEnumSet(Direction.class)));
				}
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
	 * @return The cores on the board that are blacklisted where the whole chip
	 *         is not blacklisted. Note that these are <em>physical</em>
	 *         processor IDs, not logical ones.
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

	/** @return The raw blacklist data. Read only. */
	public ByteBuffer getRawData() {
		return rawData;
	}

	private static final int MAGIC = 0x600dBeef;

	@Override
	public int hashCode() {
		return MAGIC ^ chips.hashCode() ^ cores.hashCode() ^ links.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object != null && object instanceof Blacklist) {
			return equals((Blacklist) object);
		}
		return false;
	}

	private boolean equals(Blacklist other) {
		return chips.equals(other.chips) && cores.equals(other.cores)
				&& links.equals(other.links);
	}
}
