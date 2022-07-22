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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Integer.parseInt;
import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_LINKS_PER_ROUTER;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.MAX_NUM_CORES;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSORS_PER_CHIP;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_X_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_Y_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry.getSpinn5Geometry;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.OR;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.toEnumSet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry;
import uk.ac.manchester.spinnaker.utils.CollectionUtils;

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

	private static final SpiNNakerTriadGeometry GEOM = getSpinn5Geometry();

	private static final int SPINN5_CHIPS_PER_BOARD = 48;

	private static final int COORD_BITS = 3;

	private static final int COORD_MASK = (1 << COORD_BITS) - 1;

	private static final int CORE_MASK = (1 << MAX_NUM_CORES) - 1;

	private static final int LINK_MASK = (1 << MAX_LINKS_PER_ROUTER) - 1;

	private static final int PAYLOAD_BITS =
			MAX_NUM_CORES + MAX_LINKS_PER_ROUTER;

	/**
	 * Bytes that represent the blacklist. <strong>Writability undefined.
	 * Endianness undefined.</strong> Position will be at the start of the
	 * blacklist data, which will be expected to run to the limit of the buffer
	 * (i.e., there are {@link Buffer#remaining()} bytes left). Access to this
	 * variable must be careful!
	 */
	private transient ByteBuffer rawData;

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
		ByteBuffer buf = buffer.duplicate().order(LITTLE_ENDIAN);
		rawData = buf.duplicate();
		decodeBlacklist(buf);
	}

	/**
	 * Create a blacklist from parsed data.
	 *
	 * @param deadChips
	 *            The set of chips that are dead.
	 * @param deadCores
	 *            The set of physical core IDs that are dead on live chips.
	 *            <em>Should not contain any empty sets of physical core
	 *            IDs;</em> caller should ensure.
	 * @param deadLinks
	 *            The set of link directions that are dead on live chips.
	 *            <em>Should not contain any empty sets of directions;</em>
	 *            caller should ensure.
	 */
	public Blacklist(Set<ChipLocation> deadChips,
			Map<ChipLocation, ? extends Set<Integer>> deadCores,
			Map<ChipLocation, ? extends Set<Direction>> deadLinks) {
		chips = deadChips;
		// Sort the elements in each sub-collection
		cores = deadCores.entrySet().stream().collect(
				toMap(Entry::getKey, e -> new TreeSet<>(e.getValue())));
		links = deadLinks.entrySet().stream()
				.collect(toMap(Entry::getKey, e -> copyOf(e.getValue())));
		rawData = encodeBlacklist();
	}

	private ByteBuffer encodeBlacklist() {
		ByteBuffer buf = allocate((SPINN5_CHIPS_PER_BOARD + 1) * WORD_SIZE)
				.order(LITTLE_ENDIAN);
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
					links.put(b,
							range(0, MAX_LINKS_PER_ROUTER)
									.filter(c -> (mll & (1 << c)) != 0)
									.mapToObj(Direction::byId)
									.collect(toEnumSet(Direction.class)));
				}
			}
		}
	}

	/**
	 * Create a blacklist from a string.
	 *
	 * @param blacklistText
	 *            The string to parse.
	 * @throws IllegalArgumentException
	 *             If the string is badly formatted.
	 */
	public Blacklist(String blacklistText) {
		stream(blacklistText.split("\\R+")).map(String::trim)
				// Remove blank and comment lines
				.filter(Blacklist::isRelevantLine)
				// Parse the remaining lines
				.forEach(this::parseLine);
		rawData = encodeBlacklist();
	}

	/**
	 * Create a blacklist from a text file.
	 *
	 * @param blacklistFile
	 *            The file to parse.
	 * @throws IOException
	 *             If the file can't be read from.
	 * @throws IllegalArgumentException
	 *             If the string is badly formatted.
	 */
	public Blacklist(File blacklistFile) throws IOException {
		try (FileReader r = new FileReader(requireNonNull(blacklistFile));
				BufferedReader br = new BufferedReader(r)) {
			br.lines().map(String::trim)
					// Remove blank and comment lines
					.filter(Blacklist::isRelevantLine)
					// Parse the remaining lines
					.forEach(this::parseLine);
			rawData = encodeBlacklist();
		}
	}

	private static boolean isRelevantLine(String s) {
		return !s.isEmpty() && !s.startsWith("#");
	}

	// REs from Perl code to read blacklist files

	private static final Pattern CHIP_PATTERN = compile(
			"^\\s*chip\\s+(?<x>[0-7])\\s+(?<y>[0-7])\\s*");

	private static final Pattern CORE_PATTERN = compile(
			"core\\s+(?<cores>\\S+)\\s*");

	private static final Pattern LINK_PATTERN = compile(
			"link\\s+(?<links>\\S+)\\s*");

	private static final Pattern DEAD_PATTERN = compile("dead\\s*");

	private static String deleteMatched(Matcher m) {
		// TODO Java 8 uses StringBuffer for this; WHYWHYWHY?!
		StringBuffer sb = new StringBuffer();
		m.appendReplacement(sb, "").appendTail(sb);
		return sb.toString();
	}

	private static Set<Integer> parseCommaSeparatedSet(String str) {
		return CollectionUtils.parseCommaSeparatedSet(str, Integer::parseInt);
	}

	private static <T extends Enum<T>> Set<T> parseCommaSeparatedSet(
			String str, Function<Integer, T> fun, Class<T> cls) {
		return stream(str.split(",")).map(Integer::parseInt).map(fun)
				.collect(toEnumSet(cls));
	}

	/**
	 * Parse one non-empty non-comment line of a blacklist file.
	 *
	 * @param line
	 *            The line's contents.
	 * @throws IllegalArgumentException
	 *             On most parse errors.
	 * @throws ArrayIndexOutOfBoundsException
	 *             On a bad direction.
	 */
	private void parseLine(String line) {
		Matcher m = CHIP_PATTERN.matcher(line);
		if (!m.find()) {
			throw new IllegalArgumentException("bad line: " + line);
		}
		int x = parseInt(m.group("x"));
		int y = parseInt(m.group("y"));
		ChipLocation chip = new ChipLocation(x, y);
		if (!GEOM.singleBoard().contains(chip)) {
			throw new IllegalArgumentException("bad chip coords: " + line);
		}
		String rest = deleteMatched(m);

		ChipLocation dead = null;
		Set<Integer> deadCores = null;
		Set<Direction> deadLinks = null;

		// Look for patterns at start of line while we can
		while (true) {
			m = CORE_PATTERN.matcher(rest);
			if (m.find() && deadCores == null) {
				deadCores = parseCommaSeparatedSet(m.group("cores"));
				deadCores.forEach(c -> {
					if (c < 0 || c >= PROCESSORS_PER_CHIP) {
						throw new IllegalArgumentException(
								"bad core number: " + line);
					}
				});
				rest = deleteMatched(m);
				continue;
			}

			m = LINK_PATTERN.matcher(rest);
			if (m.find() && deadLinks == null) {
				deadLinks = parseCommaSeparatedSet(m.group("links"),
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
			chips.add(dead);
			// Mask any info from lines defined above this one
			cores.remove(dead);
			links.remove(dead);
		} else if (!chips.contains(chip)) {
			if (deadCores != null && !deadCores.isEmpty()) {
				cores.computeIfAbsent(chip, k -> new HashSet<>())
						.addAll(deadCores);
			}
			if (deadLinks != null && !deadLinks.isEmpty()) {
				links.computeIfAbsent(chip, k -> noneOf(Direction.class))
						.addAll(deadLinks);
			}
		}
	}

	/**
	 * Convert the blacklist to a string in a human-readable format. This is the
	 * format understood by {@link #Blacklist(String)}.
	 * <p>
	 * Note that the result may omit information in the original blacklist, but
	 * only if that would also be ignored by the string parser.
	 *
	 * @return The string form of the blacklist.
	 * @throws RuntimeException
	 *             If something goes wrong. Not expected!
	 */
	public String render() {
		try (StringWriter sw = new StringWriter();
				BufferedWriter bw = new BufferedWriter(sw);
				PrintWriter pw = new PrintWriter(bw)) {
			render(pw);
			if (pw.checkError()) {
				// Annoying that HOW things failed gets swallowed...
				throw new RuntimeException("failed to write blacklist");
			}
			return sw.toString();
		} catch (IOException e) {
			throw new RuntimeException(
					"unexpected exception while writing blacklist to string",
					e);
		}
	}

	private void render(PrintWriter out) {
		// Don't use println(); not exactly portable on Windows
		for (ChipLocation chip : GEOM.singleBoard()) {
			if (!isChipMentioned(chip)) {
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

	public boolean isChipMentioned(ChipLocation chip) {
		return chips.contains(chip) || cores.containsKey(chip)
				|| links.containsKey(chip);
	}

	/** @return The raw blacklist data in little-endian form. Read only. */
	public ByteBuffer getRawData() {
		return rawData.asReadOnlyBuffer().order(LITTLE_ENDIAN);
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

	public String toString() {
		StringBuilder s = new StringBuilder("Blacklist(");
		s.append(chips).append(", ").append(cores).append(", ").append(links);
		return s.append(")").toString();
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeInt(rawData.remaining());
		if (rawData.hasArray()) {
			out.write(rawData.array(), rawData.position(), rawData.remaining());
		} else {
			byte[] buf = new byte[rawData.remaining()];
			rawData.duplicate().get(buf);
			out.write(buf);
		}
	}

	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		int len = in.readInt();
		byte[] buf = new byte[len];
		in.read(buf);
		rawData = ByteBuffer.wrap(buf);
	}

	static {
		Buffer.class.getClass();
	}
}
