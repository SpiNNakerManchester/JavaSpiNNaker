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
import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;
import static org.apache.commons.io.IOUtils.buffer;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.SpiNNakerTriadGeometry;
import uk.ac.manchester.spinnaker.machine.ValidP;
import uk.ac.manchester.spinnaker.utils.CollectionUtils;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

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
	@UsedInJavadocOnly(Buffer.class)
	private transient ByteBuffer rawData;

	/** The blacklisted chips. */
	private Set<@Valid ChipLocation> chips = new HashSet<>();

	/** The blacklisted cores. */
	private Map<@Valid ChipLocation, Set<@ValidP Integer>> cores =
			new HashMap<>();

	/** The blacklisted links. */
	private Map<@Valid ChipLocation, Set<Direction>> links = new HashMap<>();

	/**
	 * Create a blacklist from raw data.
	 *
	 * @param buffer
	 *            The raw data to parse.
	 */
	public Blacklist(ByteBuffer buffer) {
		var buf = requireNonNull(buffer).duplicate().order(LITTLE_ENDIAN);
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
		chips = requireNonNull(deadChips);
		// Sort the elements in each sub-collection
		cores = requireNonNull(deadCores).entrySet().stream().collect(
				toMap(Entry::getKey, e -> new TreeSet<>(e.getValue())));
		links = requireNonNull(deadLinks).entrySet().stream().collect(
				toMap(Entry::getKey, e -> EnumSet.copyOf(e.getValue())));
		rawData = encodeBlacklist();
	}

	private ByteBuffer encodeBlacklist() {
		var buf = allocate((SPINN5_CHIPS_PER_BOARD + 1) * WORD_SIZE)
				.order(LITTLE_ENDIAN);
		buf.putInt(0); // Size; filled in later
		int count = 0;
		for (int x = 0; x < SIZE_X_OF_ONE_BOARD; x++) {
			for (int y = 0; y < SIZE_Y_OF_ONE_BOARD; y++) {
				var chip = new ChipLocation(x, y);
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
		var entries = buf.asIntBuffer();
		int len = entries.get();
		var done = new HashSet<ChipLocation>();

		for (int i = 0; i < len; i++) {
			int entry = entries.get();

			// get board coordinates
			int bx = (entry >> (PAYLOAD_BITS + COORD_BITS)) & COORD_MASK;
			int by = (entry >> PAYLOAD_BITS) & COORD_MASK;
			var b = new ChipLocation(bx, by);

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
		requireNonNull(blacklistText, "blacklist text should not be null")
				.lines().map(String::strip)
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
		try (var r =
				buffer(new FileReader(
						requireNonNull(blacklistFile,
								"blacklist filename should not be null"),
						UTF_8))) {
			r.lines().map(String::strip)
					// Remove blank and comment lines
					.filter(Blacklist::isRelevantLine)
					// Parse the remaining lines
					.forEach(this::parseLine);
			rawData = encodeBlacklist();
		}
	}

	private static boolean isRelevantLine(String s) {
		return !s.isBlank() && !s.startsWith("#");
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
		var sb = new StringBuilder();
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
		var m = CHIP_PATTERN.matcher(line);
		if (!m.find()) {
			throw new IllegalArgumentException("bad line: " + line);
		}
		int x = parseInt(m.group("x"));
		int y = parseInt(m.group("y"));
		var chip = new ChipLocation(x, y);
		if (!GEOM.singleBoard().contains(chip)) {
			throw new IllegalArgumentException("bad chip coords: " + line);
		}
		var rest = deleteMatched(m);

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
			if (!rest.isBlank()) {
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
				cores.computeIfAbsent(chip, __ -> new HashSet<>())
						.addAll(deadCores);
			}
			if (deadLinks != null && !deadLinks.isEmpty()) {
				links.computeIfAbsent(chip,
						__ -> EnumSet.noneOf(Direction.class))
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
		try (var f = new Formatter()) {
			render(f);
			return f.toString();
		}
	}

	private void render(Formatter out) {
		for (var chip : GEOM.singleBoard()) {
			if (!isChipMentioned(chip)) {
				continue;
			}
			out.format("chip %d %d", chip.getX(), chip.getY());
			if (chips.contains(chip)) {
				out.format(" dead\n");
			} else {
				if (cores.containsKey(chip)) {
					out.format(" core ");
					var sep = "";
					for (var id : cores.get(chip)) {
						out.format(sep).format(id.toString());
						sep = ",";
					}
				}
				if (links.containsKey(chip)) {
					out.format(" link ");
					var sep = "";
					for (var d : links.get(chip)) {
						out.format(sep).format(Integer.toString(d.id));
						sep = ",";
					}
				}
				out.format("\n");
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

	/**
	 * Test if a chip is known about by the blacklist.
	 *
	 * @param chip
	 *            The chip to look for. Coordinates must be board-local.
	 * @return Whether the chip is mentioned in the blacklist. That could be if
	 *         it is blacklisted, if it has a blacklisted core, of if one of its
	 *         links is blacklisted.
	 */
	public boolean isChipMentioned(ChipLocation chip) {
		return chips.contains(chip) || cores.containsKey(chip)
				|| links.containsKey(chip);
	}

	/** @return The raw blacklist data in little-endian form. Read only. */
	public ByteBuffer getRawData() {
		return rawData.asReadOnlyBuffer().order(LITTLE_ENDIAN);
	}

	@Override
	public int hashCode() {
		return Objects.hash(chips, cores, links);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Blacklist) {
			var other = (Blacklist) object;
			return chips.equals(other.chips) && cores.equals(other.cores)
					&& links.equals(other.links);
		}
		return false;
	}

	@Override
	public String toString() {
		var s = new StringBuilder("Blacklist(");
		s.append(chips).append(", ").append(cores).append(", ").append(links);
		return s.append(")").toString();
	}

	/**
	 * Write this object to the stream. This is standard except for the special
	 * handling of the raw data.
	 *
	 * @param out
	 *            Where to write to.
	 * @throws IOException
	 *             If output fails.
	 * @see ObjectOutputStream#defaultWriteObject()
	 */
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

	/**
	 * Set this object up by reading from the stream. This is standard except
	 * for the special handling of the raw data.
	 *
	 * @param in
	 *            Where to read from.
	 * @throws IOException
	 *             If input fails.
	 * @throws ClassNotFoundException
	 *             if the class of a serialized object could not be found.
	 * @see ObjectInputStream#defaultReadObject()
	 */
	private void readObject(ObjectInputStream in)
			throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		int len = in.readInt();
		var buf = new byte[len];
		in.read(buf);
		rawData = wrap(buf).order(LITTLE_ENDIAN);
	}
}
