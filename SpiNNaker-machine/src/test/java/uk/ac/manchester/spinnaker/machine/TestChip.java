/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian-B
 */
public class TestChip {
	private static final ChipLocation LOCATION_00 = new ChipLocation(0, 0);

	private static final ChipLocation LOCATION_01 = new ChipLocation(0, 1);

	private static final ChipLocation LOCATION_10 = new ChipLocation(1, 0);

	private static final ChipLocation LOCATION_11 = new ChipLocation(1, 1);

	private static final Link LINK_00_01 =
			new Link(LOCATION_00, Direction.NORTH, LOCATION_01);

	//private static final Link LINK_00_01a =
	//		new Link(LOCATION_00, Direction.NORTH, LOCATION_01);

	private static final Link LINK_00_10 =
			new Link(LOCATION_00, Direction.WEST, LOCATION_10);

	//private static final Link LINK_01_01 =
	//		new Link(LOCATION_01, Direction.SOUTH, LOCATION_01);

	private Router createRouter() {
		var links = List.of(LINK_00_10, LINK_00_01);
		return new Router(links);
	}

	private static final byte[] LOCALHOST_ADDRESS_BYTES = {127, 0, 0, 0};

	private InetAddress createInetAddress() throws UnknownHostException {
		return InetAddress.getByAddress(LOCALHOST_ADDRESS_BYTES);
	}

	private List<Processor> getProcessors() {
		return List.of(//
				Processor.factory(1), //
				Processor.factory(2, true), //
				Processor.factory(4));
	}

	private List<Processor> getProcessors(Processor extra) {
		return List.of(//
				Processor.factory(1), //
				Processor.factory(2, true), //
				Processor.factory(4), extra);
	}

	@Test
	public void testChipBasic() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6);
		var chip = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), tags, LOCATION_11);
		assertEquals(0, chip.getX());
		assertEquals(0, chip.getY());
		assertEquals(3, chip.nProcessors());
		assertEquals(2, chip.nUserProcessors());
		assertFalse(chip.hasUserProcessor(2));
		assertTrue(chip.hasUserProcessor(4));
		assertFalse(chip.hasUserProcessor(3));
		assertNull(chip.getUserProcessor(2));
		assertEquals(Processor.factory(4), chip.getUserProcessor(4));
		assertNull(chip.getUserProcessor(3));
		// contains check that is has exactly these elements in order
		assertThat(chip.userProcessors(),
				contains(Processor.factory(1), Processor.factory(4)));
	}

	@Test
	public void testDefault() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6, 7);
		var chip = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), LOCATION_11);
		assertEquals(tags, chip.getTagIds());
	}

	@Test
	public void testChipMonitors() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6);
		var chip = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), tags, LOCATION_11);
		var result = chip.getFirstUserProcessor();
		assertEquals(Processor.factory(1), result);
		assertEquals(2, chip.nUserProcessors());
	}

	/*
	 * Test of toString method, of class Chip.
	 */
	@Test
	public void testToString() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6);
		var chip1 = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), tags, LOCATION_11);
		var chip2 = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), tags, LOCATION_11);
		assertEquals(chip1.toString(), chip2.toString());
	}

	@Test
	public void testRepeatMonitor() {
		var processors = getProcessors(Processor.factory(2, false));
		assertThrows(IllegalArgumentException.class, () -> {
			var chip = new Chip(LOCATION_00, processors, createRouter(), 100,
					createInetAddress(), LOCATION_11);
			assertNotNull(chip);
		});
	}

	@Test
	public void testRepeatUser() {
		var processors = getProcessors(Processor.factory(4, true));
		assertThrows(IllegalArgumentException.class, () -> {
			var chip = new Chip(LOCATION_00, processors, createRouter(), 100,
					createInetAddress(), LOCATION_11);
			assertNotNull(chip);
		});
	}

	@Test
	public void testAsLocation() throws UnknownHostException {
		var chip1 = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), null, LOCATION_11);
		assertEquals(LOCATION_00, chip1.asChipLocation());
	}

	@Test
	public void testGet() throws UnknownHostException {
		var chip1 = new Chip(new ChipLocation(3, 4), getProcessors(),
				createRouter(), 100, createInetAddress(), LOCATION_11);
		assertEquals(3, chip1.getX());
		assertEquals(4, chip1.getY());
		assertEquals(new ChipLocation(3, 4), chip1.asChipLocation());
	}

	@Test
	public void testDefault1() {
		var chip = new Chip(LOCATION_10, createRouter(), null, LOCATION_11);
		assertEquals(LOCATION_10, chip.asChipLocation());
		assertEquals(17, chip.nUserProcessors());
		assertEquals(18, chip.nProcessors());
		assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
		assertTrue(chip.getTagIds().isEmpty());
	}

	@Test
	public void testDefault2() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6, 7);
		var chip = new Chip(LOCATION_00, createRouter(), createInetAddress(),
				LOCATION_11);
		assertEquals(LOCATION_00, chip.asChipLocation());
		assertEquals(17, chip.nUserProcessors());
		assertEquals(18, chip.nProcessors());
		assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
		assertEquals(tags, chip.getTagIds());
	}

	@Test
	public void testLinksVirtualMachine() throws UnknownHostException {
		var chip = new Chip(LOCATION_00, createRouter(), createInetAddress(),
				LOCATION_11);
		final var values = chip.router.links();
		assertEquals(2, values.size());
		assertThrows(UnsupportedOperationException.class, () -> {
			values.remove(LINK_00_01);
		});
		var values2 = chip.router.links();
		assertEquals(2, values2.size());
		var iterator = values2.iterator();
		assertEquals(LINK_00_01, iterator.next());
		assertEquals(LINK_00_10, iterator.next());
		assertFalse(iterator.hasNext());
		assertTrue(chip.router.hasLink(Direction.NORTH));
	}

}
