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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

class TestBlacklist {
	private static final ChipLocation C00 = new ChipLocation(0, 0);

	private static final ChipLocation C01 = new ChipLocation(0, 1);

	private static final ChipLocation C10 = new ChipLocation(1, 0);

	private static final ChipLocation C11 = new ChipLocation(1, 1);

	private static final ChipLocation C77 = new ChipLocation(7, 7);

	private static final String EXAMPLE_BLACKLIST_FILE =
			"uk/ac/manchester/spinnaker/alloc/bmp/example.blacklist";

	@SafeVarargs
	private static <T> Set<T> set(T... args) {
		return new HashSet<>(Arrays.asList(args));
	}

	@SuppressWarnings("unchecked")
	private static <T> Map<ChipLocation, Set<T>> map(Object... args) {
		Map<ChipLocation, Set<T>> map = new HashMap<>();
		for (int i = 0; i < args.length; i += 2) {
			ChipLocation key = (ChipLocation) args[i];
			Set<T> value = (Set<T>) args[i + 1];
			map.put(key, value);
		}
		return map;
	}

	private static byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
		}
		return baos.toByteArray();
	}

	private static <T> T deserialize(byte[] bytes, Class<T> cls)
			throws ClassNotFoundException, IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		try (ObjectInputStream ois = new ObjectInputStream(bais)) {
			return cls.cast(ois.readObject());
		}
	}

	@Nested
	class MechanicalSerialization {
		@Test
		void javaForm() throws IOException, ClassNotFoundException {
			Blacklist blIn = new Blacklist(set(C11), map(C00, set(3)),
					map(C00, set(WEST)));

			byte[] serialForm = serialize(blIn);

			Blacklist blOut = deserialize(serialForm, Blacklist.class);

			assertEquals(blIn, blOut);
		}

		@Test
		void spinnakerForm() {
			Blacklist blIn = new Blacklist(set(C11), map(C00, set(3)),
					map(C00, set(SOUTHWEST)));

			ByteBuffer raw = blIn.getRawData();

			// Test that we know what's in the raw data
			assertEquals(ByteOrder.LITTLE_ENDIAN, raw.order());
			assertEquals(12, raw.remaining());
			IntBuffer words = raw.asIntBuffer();
			assertEquals(2, words.get());
			assertEquals(0x0400008, words.get()); // chip 0,0 core 3 link 4
			assertEquals(0x903ffff, words.get()); // chip 1,1 dead
			// No data after that
			assertThrows(BufferUnderflowException.class, words::get);

			// Parse
			Blacklist blOut = new Blacklist(raw);

			assertEquals(blIn, blOut);
			assertEquals(set(C11), blOut.getChips());
			assertEquals(map(C00, set(3)), blOut.getCores());
			assertEquals(map(C00, set(SOUTHWEST)), blOut.getLinks());
		}
	}

	@Nested
	class WithStrings {
		@Test
		void parseEmptyBlacklist() {
			String blData = "";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseOneWholeDeadChip() {
			String blData = "chip 0 0 dead";

			Blacklist bl = new Blacklist(blData);

			assertEquals(singleton(C00), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoreAndLink() {
			String blData = "chip 0 0 core 2 link 3";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(singletonMap(C00, singleton(2)), bl.getCores());
			assertEquals(singletonMap(C00, singleton(WEST)), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCores() {
			String blData = "chip 0 0 core 2,16";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(singletonMap(C00, set(16, 2)), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseOneChipDeadLinks() {
			String blData = "chip 0 0 link 0,3,5,2";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(singletonMap(C00, set(NORTH, SOUTH, EAST, WEST)),
					bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoresAndLinks() {
			String blData = "chip 0 0 core 2,3 link 3,0";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(singletonMap(C00, set(3, 2)), bl.getCores());
			assertEquals(singletonMap(C00, set(EAST, WEST)), bl.getLinks());
		}

		@Test
		void parseOneChipAllParts() {
			String blData = "chip 0 0 core 2 link 3 dead";

			Blacklist bl = new Blacklist(blData);

			assertEquals(singleton(C00), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseOneChipAllPartsAlternateOrdering() {
			String blData = "chip 0 0 dead link 3 core 2";

			Blacklist bl = new Blacklist(blData);

			assertEquals(singleton(C00), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseSeveralChips() {
			String blData = "chip 0 0 core 2\nchip 0 1 link 0\nchip 1 0 dead";

			Blacklist bl = new Blacklist(blData);

			assertEquals(singleton(C10), bl.getChips());
			assertEquals(singletonMap(C00, singleton(2)), bl.getCores());
			assertEquals(singletonMap(C01, singleton(EAST)), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoresAndLinksTwoLines() {
			String blData = "chip 0 0 core 2,3\nchip 0 0 link 3,0";

			Blacklist bl = new Blacklist(blData);

			assertEquals(emptySet(), bl.getChips());
			assertEquals(singletonMap(C00, set(3, 2)), bl.getCores());
			assertEquals(singletonMap(C00, set(EAST, WEST)), bl.getLinks());
		}

		@Test
		void parseWhitespaceCommentStrip() {
			String blData =
					"#comment\n\n  \n   chip    0    0    dead   \n# comment";

			Blacklist bl = new Blacklist(blData);

			assertEquals(singleton(C00), bl.getChips());
			assertEquals(emptyMap(), bl.getCores());
			assertEquals(emptyMap(), bl.getLinks());
		}

		@Test
		void parseJunkLine() {
			String blData = "garbage\nchip 0 0 dead";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: garbage", e.getMessage());
		}

		@Test
		void parseJunkPrefix() {
			String blData = "garbage chip 0 0 dead";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: garbage chip 0 0 dead", e.getMessage());
		}

		@Test
		void parseJunkSuffix() {
			String blData = "chip 0 0 dead junk";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 dead junk", e.getMessage());
		}

		@Test
		void parseDoubleDead() {
			String blData = "chip 0 0 dead dead";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 dead dead", e.getMessage());
		}

		@Test
		void parseDoubleCore() {
			String blData = "chip 0 0 core 1 core 2";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 core 1 core 2", e.getMessage());
		}

		@Test
		void parseDoubleLink() {
			String blData = "chip 0 0 link 1 link 2";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 link 1 link 2", e.getMessage());
		}

		@Test
		void parseBadChipCoords() {
			String blData = "chip 0 7 dead";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad chip coords: chip 0 7 dead", e.getMessage());
		}

		@Test
		void parseBadCoreNumber() {
			String blData = "chip 0 0 core 42";

			Exception e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad core number: chip 0 0 core 42", e.getMessage());
		}

		@Test
		void parseBadLinkNumber() {
			String blData = "chip 0 0 link 42";

			Exception e =
					assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
						new Blacklist(blData);
					});
			assertEquals("direction ID 42 not in range 0 to 5", e.getMessage());
		}

		@Test
		void printSimpleToString() {
			Blacklist bl = new Blacklist(set(C01, C11), map(C10, set(1, 2, 3)),
					map(C77, set(NORTH, SOUTH, EAST, WEST)));

			String s = bl.render();

			assertEquals("chip 0 1 dead\nchip 1 0 core 1,2,3\n"
					+ "chip 1 1 dead\nchip 7 7 link 0,2,3,5\n", s);
		}

		@Test
		void printMessyToString() {
			Blacklist bl = new Blacklist(set(C01, C11), map(C01, set(1, 2, 3)),
					map(C11, set(NORTH, SOUTH, EAST, WEST)));

			String s = bl.render();

			assertEquals("chip 0 1 dead\nchip 1 1 dead\n", s);
		}

		@Test
		void printEmptyToString() {
			Blacklist bl = new Blacklist(set(), map(), map());

			String s = bl.render();

			assertEquals("", s);
		}
	}

	@Nested
	class WithFiles {
		/** The example blacklist file. */
		private File blf = new File(getClass().getClassLoader()
				.getResource(EXAMPLE_BLACKLIST_FILE).getFile());

		@Test
		void readFile() throws IOException {
			Blacklist bl = new Blacklist(blf);

			assertEquals(singleton(C11), bl.getChips());
			assertEquals(map(C10, set(2, 3), C77, set(10, 17)), bl.getCores());
			assertEquals(
					map(C10, set(SOUTHWEST, SOUTH), C77, set(NORTHEAST, SOUTH)),
					bl.getLinks());
		}
	}
}
