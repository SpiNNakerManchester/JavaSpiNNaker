/*
 * Copyright (c) 2022-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
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
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;

@SuppressWarnings("ClassCanBeStatic")
class TestBlacklist {
	private static final ChipLocation C00 = new ChipLocation(0, 0);

	private static final ChipLocation C01 = new ChipLocation(0, 1);

	private static final ChipLocation C10 = new ChipLocation(1, 0);

	private static final ChipLocation C11 = new ChipLocation(1, 1);

	private static final ChipLocation C77 = new ChipLocation(7, 7);

	private static final String EXAMPLE_BLACKLIST_FILE =
			"uk/ac/manchester/spinnaker/alloc/bmp/example.blacklist";

	private static byte[] serialize(Object obj) throws IOException {
		var baos = new ByteArrayOutputStream();
		try (var oos = new ObjectOutputStream(baos)) {
			oos.writeObject(obj);
		}
		return baos.toByteArray();
	}

	private static <T> T deserialize(byte[] bytes, Class<T> cls)
			throws ClassNotFoundException, IOException {
		var bais = new ByteArrayInputStream(bytes);
		try (var ois = new ObjectInputStream(bais)) {
			return cls.cast(ois.readObject());
		}
	}

	@Nested
	class MechanicalSerialization {
		@Test
		void javaForm() throws IOException, ClassNotFoundException {
			var blIn = new Blacklist(Set.of(C11), Map.of(C00, Set.of(3)),
					Map.of(C00, EnumSet.of(WEST)));

			var serialForm = serialize(blIn);

			var blOut = deserialize(serialForm, Blacklist.class);

			assertEquals(blIn, blOut);
		}

		@Test
		void spinnakerForm() {
			var blIn = new Blacklist(Set.of(C11), Map.of(C00, Set.of(3)),
					Map.of(C00, EnumSet.of(SOUTHWEST)));

			var raw = blIn.getRawData();

			// Test that we know what's in the raw data
			assertEquals(LITTLE_ENDIAN, raw.order());
			assertEquals(12, raw.remaining());
			var words = raw.asIntBuffer();
			assertEquals(2, words.get());
			assertEquals(0x0400008, words.get()); // chip 0,0 core 3 link 4
			assertEquals(0x903ffff, words.get()); // chip 1,1 dead
			// No data after that
			assertThrows(BufferUnderflowException.class, words::get);

			// Parse
			var blOut = new Blacklist(raw);

			assertEquals(blIn, blOut);
			assertEquals(Set.of(C11), blOut.getChips());
			assertEquals(Map.of(C00, Set.of(3)), blOut.getCores());
			assertEquals(Map.of(C00, EnumSet.of(SOUTHWEST)), blOut.getLinks());
		}
	}

	@Nested
	class WithStrings {
		@Test
		void parseEmptyBlacklist() {
			var blData = "";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseOneWholeDeadChip() {
			var blData = "chip 0 0 dead";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(C00), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoreAndLink() {
			var blData = "chip 0 0 core 2 link 3";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(C00, Set.of(2)), bl.getCores());
			assertEquals(Map.of(C00, EnumSet.of(WEST)), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCores() {
			var blData = "chip 0 0 core 2,16";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(C00, Set.of(16, 2)), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseOneChipDeadLinks() {
			var blData = "chip 0 0 link 0,3,5,2";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(C00, EnumSet.of(NORTH, SOUTH, EAST, WEST)),
					bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoresAndLinks() {
			var blData = "chip 0 0 core 2,3 link 3,0";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(C00, Set.of(3, 2)), bl.getCores());
			assertEquals(Map.of(C00, EnumSet.of(EAST, WEST)), bl.getLinks());
		}

		@Test
		void parseOneChipAllParts() {
			var blData = "chip 0 0 core 2 link 3 dead";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(C00), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseOneChipAllPartsAlternateOrdering() {
			var blData = "chip 0 0 dead link 3 core 2";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(C00), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseSeveralChips() {
			var blData = "chip 0 0 core 2\nchip 0 1 link 0\nchip 1 0 dead";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(C10), bl.getChips());
			assertEquals(Map.of(C00, Set.of(2)), bl.getCores());
			assertEquals(Map.of(C01, EnumSet.of(EAST)), bl.getLinks());
		}

		@Test
		void parseOneChipDeadCoresAndLinksTwoLines() {
			var blData = "chip 0 0 core 2,3\nchip 0 0 link 3,0";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(), bl.getChips());
			assertEquals(Map.of(C00, Set.of(3, 2)), bl.getCores());
			assertEquals(Map.of(C00, EnumSet.of(EAST, WEST)), bl.getLinks());
		}

		@Test
		void parseWhitespaceCommentStrip() {
			var blData =
					"#comment\n\n  \n   chip    0    0    dead   \n# comment";

			var bl = new Blacklist(blData);

			assertEquals(Set.of(C00), bl.getChips());
			assertEquals(Map.of(), bl.getCores());
			assertEquals(Map.of(), bl.getLinks());
		}

		@Test
		void parseJunkLine() {
			var blData = "garbage\nchip 0 0 dead";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: garbage", e.getMessage());
		}

		@Test
		void parseJunkPrefix() {
			var blData = "garbage chip 0 0 dead";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: garbage chip 0 0 dead", e.getMessage());
		}

		@Test
		void parseJunkSuffix() {
			var blData = "chip 0 0 dead junk";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 dead junk", e.getMessage());
		}

		@Test
		void parseDoubleDead() {
			var blData = "chip 0 0 dead dead";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 dead dead", e.getMessage());
		}

		@Test
		void parseDoubleCore() {
			var blData = "chip 0 0 core 1 core 2";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 core 1 core 2", e.getMessage());
		}

		@Test
		void parseDoubleLink() {
			var blData = "chip 0 0 link 1 link 2";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad line: chip 0 0 link 1 link 2", e.getMessage());
		}

		@Test
		void parseBadChipCoords() {
			var blData = "chip 0 7 dead";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad chip coords: chip 0 7 dead", e.getMessage());
		}

		@Test
		void parseBadCoreNumber() {
			var blData = "chip 0 0 core 42";

			var e = assertThrows(IllegalArgumentException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("bad core number: chip 0 0 core 42", e.getMessage());
		}

		@Test
		void parseBadLinkNumber() {
			var blData = "chip 0 0 link 42";

			var e = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
				new Blacklist(blData);
			});
			assertEquals("direction ID 42 not in range 0 to 5", e.getMessage());
		}

		@Test
		void printSimpleToString() {
			var bl = new Blacklist(Set.of(C01, C11),
					Map.of(C10, Set.of(1, 2, 3)),
					Map.of(C77, EnumSet.of(NORTH, SOUTH, EAST, WEST)));

			var s = bl.render();

			assertEquals("chip 0 1 dead\nchip 1 0 core 1,2,3\n"
					+ "chip 1 1 dead\nchip 7 7 link 0,2,3,5\n", s);
		}

		@Test
		void printMessyToString() {
			var bl = new Blacklist(Set.of(C01, C11),
					Map.of(C01, Set.of(1, 2, 3)),
					Map.of(C11, EnumSet.of(NORTH, SOUTH, EAST, WEST)));

			var s = bl.render();

			assertEquals("chip 0 1 dead\nchip 1 1 dead\n", s);
		}

		@Test
		void printEmptyToString() {
			var bl = new Blacklist(Set.of(), Map.of(), Map.of());

			var s = bl.render();

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
			var bl = new Blacklist(blf);

			assertEquals(Set.of(C11), bl.getChips());
			assertEquals(Map.of(C10, Set.of(2, 3), C77, Set.of(10, 17)),
					bl.getCores());
			assertEquals(Map.of(C10, EnumSet.of(SOUTHWEST, SOUTH), C77,
					EnumSet.of(NORTHEAST, SOUTH)), bl.getLinks());
		}
	}
}
