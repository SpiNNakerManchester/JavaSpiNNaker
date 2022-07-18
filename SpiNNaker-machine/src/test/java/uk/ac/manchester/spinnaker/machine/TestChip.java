/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

	@Test
	public void testChipBasic() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6);
		var chip = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), false, tags, LOCATION_11);
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
		assertFalse(chip.virtual);
	}

	@Test
	public void testChipMonitors() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6);
		var chip = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), false, tags, LOCATION_11);
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
				createInetAddress(), false, tags, LOCATION_11);
		var chip2 = new Chip(LOCATION_00, getProcessors(), createRouter(), 100,
				createInetAddress(), false, tags, LOCATION_11);
		assertEquals(chip1.toString(), chip2.toString());
	}

	@Test
	public void testRepeatMonitor() throws UnknownHostException {
		var processors = new ArrayList<>(getProcessors());
		processors.add(Processor.factory(2, false));
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			var chip = new Chip(ChipLocation.ZERO_ZERO, processors,
					createRouter(), 100, createInetAddress(), LOCATION_11);
		});
	}

	@Test
	public void testRepeatUser() throws UnknownHostException {
		var processors = new ArrayList<>(getProcessors());
		processors.add(Processor.factory(4, true));
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			var chip = new Chip(ChipLocation.ZERO_ZERO, processors,
					createRouter(), 100, createInetAddress(), LOCATION_11);
		});
	}

	@Test
	public void testAsLocation() throws UnknownHostException {
		var chip1 = new Chip(ChipLocation.ZERO_ZERO, getProcessors(),
				createRouter(), 100, createInetAddress(), false, null,
				LOCATION_11);
		assertEquals(ChipLocation.ZERO_ZERO, chip1.asChipLocation());
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
	public void testDefault1() throws UnknownHostException {
		var chip = new Chip(ChipLocation.ONE_ZERO, createRouter(), null,
				LOCATION_11);
		assertEquals(ChipLocation.ONE_ZERO, chip.asChipLocation());
		assertFalse(chip.virtual, "Chips on virtual nmachine are not virtual!");
		assertEquals(17, chip.nUserProcessors());
		assertEquals(18, chip.nProcessors());
		assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
		assert (chip.getTagIds().isEmpty());
	}

	@Test
	public void testDefault2() throws UnknownHostException {
		var tags = List.of(1, 2, 3, 4, 5, 6, 7);
		var chip = new Chip(ChipLocation.ZERO_ZERO, createRouter(),
				createInetAddress(), LOCATION_11);
		assertEquals(ChipLocation.ZERO_ZERO, chip.asChipLocation());
		assertFalse(chip.virtual, "Chips on virtual nmachine are not virtual!");
		assertEquals(17, chip.nUserProcessors());
		assertEquals(18, chip.nProcessors());
		assertEquals(MachineDefaults.SDRAM_PER_CHIP, chip.sdram);
		assertEquals(tags, chip.getTagIds());
	}

	@Test
	public void testLinksVirtualMachine() throws UnknownHostException {
		var chip = new Chip(ChipLocation.ZERO_ZERO, createRouter(),
				createInetAddress(), LOCATION_11);
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
		assert (chip.router.hasLink(Direction.NORTH));
	}

}
