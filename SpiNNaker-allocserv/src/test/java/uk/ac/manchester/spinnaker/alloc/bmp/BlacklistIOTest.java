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

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.deserialize;
import static uk.ac.manchester.spinnaker.alloc.IOUtils.serialize;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.messages.bmp.Blacklist;

class BlacklistIOTest {
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

	@Test
	void readEmptyBlacklistFromString() throws IOException {
		String blData = "";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(emptySet(), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistChipFromString() throws IOException {
		String blData = "chip 0 0 dead";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(singleton(new ChipLocation(0, 0)), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistCoreLinkFromString() throws IOException {
		String blData = "chip 0 0 core 2 link 3";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(emptySet(), bl.getChips());
		assertEquals(singletonMap(new ChipLocation(0, 0), singleton(2)),
				bl.getCores());
		assertEquals(singletonMap(new ChipLocation(0, 0), singleton(WEST)),
				bl.getLinks());
	}

	@Test
	void readBlacklistMultiCoreFromString() throws IOException {
		String blData = "chip 0 0 core 2,16";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(emptySet(), bl.getChips());
		assertEquals(singletonMap(new ChipLocation(0, 0), set(16, 2)),
				bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistMultiLinkFromString() throws IOException {
		String blData = "chip 0 0 link 0,3,5,2";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(emptySet(), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(singletonMap(new ChipLocation(0, 0),
				set(NORTH, SOUTH, EAST, WEST)), bl.getLinks());
	}

	@Test
	void readBlacklistMultiCoreLinkFromString() throws IOException {
		String blData = "chip 0 0 core 2,3 link 3,0";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(emptySet(), bl.getChips());
		assertEquals(singletonMap(new ChipLocation(0, 0), set(3, 2)),
				bl.getCores());
		assertEquals(singletonMap(new ChipLocation(0, 0), set(EAST, WEST)),
				bl.getLinks());
	}

	@Test
	void readBlacklistChipCoreLinkFromString() throws IOException {
		String blData = "chip 0 0 core 2 link 3 dead";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(singleton(new ChipLocation(0, 0)), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistChipCoreLinkNonStandardOrderFromString()
			throws IOException {
		String blData = "chip 0 0 dead link 3 core 2";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(singleton(new ChipLocation(0, 0)), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistMultipleChipCoreLinkFromString() throws IOException {
		String blData = "chip 0 0 core 2\nchip 0 1 link 0\nchip 1 0 dead";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(singleton(new ChipLocation(1, 0)), bl.getChips());
		assertEquals(singletonMap(new ChipLocation(0, 0), singleton(2)),
				bl.getCores());
		assertEquals(singletonMap(new ChipLocation(0, 1), singleton(EAST)),
				bl.getLinks());
	}

	@Test
	void readBlacklistWhitespaceCommentFromString() throws IOException {
		String blData =
				"#comment\n\n  \n   chip    0    0    dead   \n# comment";

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.parseBlacklist(blData);

		assertEquals(singleton(new ChipLocation(0, 0)), bl.getChips());
		assertEquals(emptyMap(), bl.getCores());
		assertEquals(emptyMap(), bl.getLinks());
	}

	@Test
	void readBlacklistGarbageFromString() throws IOException {
		String blData = "garbage\nchip 0 0 dead";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: garbage", e.getMessage());
	}

	@Test
	void readBlacklistLeadingGarbageFromString() throws IOException {
		String blData = "garbage chip 0 0 dead";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: garbage chip 0 0 dead", e.getMessage());
	}

	@Test
	void readBlacklistTrailingGarbageFromString() throws IOException {
		String blData = "chip 0 0 dead junk";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: chip 0 0 dead junk", e.getMessage());
	}

	@Test
	void readBlacklistDoubleDeadFromString() throws IOException {
		String blData = "chip 0 0 dead dead";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: chip 0 0 dead dead", e.getMessage());
	}

	@Test
	void readBlacklistDoubleCoreFromString() throws IOException {
		String blData = "chip 0 0 core 1 core 2";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: chip 0 0 core 1 core 2", e.getMessage());
	}

	@Test
	void readBlacklistDoubleLinkFromString() throws IOException {
		String blData = "chip 0 0 link 1 link 2";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad line: chip 0 0 link 1 link 2", e.getMessage());
	}

	@Test
	void readBlacklistBadChipLocFromString() throws IOException {
		String blData = "chip 0 7 dead";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad chip coords: chip 0 7 dead", e.getMessage());
	}

	@Test
	void readBlacklistBadCoreNumFromString() throws IOException {
		String blData = "chip 0 0 core 42";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("bad core number: chip 0 0 core 42", e.getMessage());
	}

	@Test
	void readBlacklistBadLinkNumFromString() throws IOException {
		String blData = "chip 0 0 link 42";

		BlacklistIO blio = new BlacklistIO();
		Exception e = assertThrows(ArrayIndexOutOfBoundsException.class, () -> {
			blio.parseBlacklist(blData);
		});
		assertEquals("Index 42 out of bounds for length 6", e.getMessage());
	}

	@Test
	void readBlacklistFromFile() throws IOException {
		String filename =
				"uk/ac/manchester/spinnaker/alloc/bmp/example.blacklist";
		File blf = new File(BlacklistIOTest.class.getClassLoader()
				.getResource(filename).getFile());

		BlacklistIO blio = new BlacklistIO();
		Blacklist bl = blio.readBlacklistFile(blf);

		assertEquals(singleton(new ChipLocation(1, 1)), bl.getChips());
		assertEquals(map(new ChipLocation(1, 0), set(2, 3),
				new ChipLocation(7, 7), set(10, 17)), bl.getCores());
		assertEquals(
				map(new ChipLocation(1, 0), set(SOUTHWEST, SOUTH),
						new ChipLocation(7, 7), set(NORTHEAST, SOUTH)),
				bl.getLinks());
	}

	@Test
	void serializeDeserialize() throws IOException, ClassNotFoundException {
		Blacklist blIn = new Blacklist(set(new ChipLocation(1, 1)),
				map(new ChipLocation(0, 0), set(3)),
				map(new ChipLocation(0, 0), set(WEST)));

		byte[] serialForm = serialize(blIn);

		Blacklist blOut = deserialize(serialForm, Blacklist.class);

		assertEquals(blIn, blOut);
	}
}
