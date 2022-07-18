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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;

import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 *
 * @author Christian-B
 */
public class TestMachine {

	public TestMachine() {
	}

	private static final ChipLocation CHIP00 = new ChipLocation(0, 0);

	private static final ChipLocation CHIP01 = new ChipLocation(0, 1);

	private static final ChipLocation CHIP10 = new ChipLocation(1, 0);

	private static final ChipLocation CHIP11 = new ChipLocation(1, 1);

	private static final ChipLocation CHIP20 = new ChipLocation(2, 0);

	private static final Link LINK00_01 =
			new Link(CHIP00, Direction.NORTH, CHIP01);

	private static final Link LINK01_11 =
			new Link(CHIP01, Direction.SOUTH, CHIP11);

	private static final Link LINK11_20 =
			new Link(CHIP11, Direction.EAST, CHIP20);

	private static final Link LINK10_30 =
			new Link(CHIP10, Direction.WEST, CHIP01);

	private static final List<Link> LINKS =
			List.of(LINK00_01, LINK01_11, LINK11_20, LINK10_30);

	private static final Router ROUTER = new Router(LINKS);

	private static final int SDRAM = 100;

	private static final ChipLocation BOOT_CHIP = CHIP00;

	private static final byte[] BYTES = {
		(byte) 192, (byte) 162, (byte) 240, (byte) 253
	};

	private static final byte[] BYTES00 = {127, 0, 0, 0};

	private static final byte[] BYTES84 = {127, 0, 8, 4};

	private static final byte[] BYTES48 = {127, 0, 4, 8};

	private List<Processor> createProcessors() {
		var processors = new ArrayList<Processor>();
		processors.add(Processor.factory(0));
		processors.add(Processor.factory(1));
		processors.add(Processor.factory(2));
		processors.add(Processor.factory(3, true));
		for (int i = 4; i < 18; i++) {
			processors.add(Processor.factory(i));
		}
		return processors;
	}

	private List<Chip> createdChips(List<Processor> processors)
			throws UnknownHostException {
		var address = InetAddress.getByAddress(BYTES);
		var chips = new ArrayList<Chip>();
		for (int x = 0; x < 5; x++) {
			for (int y = 0; y < 5; y++) {
				chips.add(new Chip(new ChipLocation(x, y), processors, ROUTER,
						SDRAM, address, BOOT_CHIP));
			}
		}
		return chips;
	}

	private Router createRouter(ChipLocation source,
			Collection<ChipLocation> all) {
		var links = new ArrayList<Link>();
		for (var direction : Direction.values()) {
			int destX = source.getX() + direction.xChange;
			int destY = source.getY() + direction.yChange;
			if (destX >= 0 && destY >= 0) {
				var destination = new ChipLocation(destX, destY);
				if (all.contains(destination)) {
					links.add(new Link(source, direction,
							new ChipLocation(destX, destY)));
				}
			}
		}
		return new Router(links);
	}

	@Test
	public void testCreateNewMachine() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		var address = InetAddress.getByAddress(BYTES);

		var instance =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);

		assertEquals(7, instance.maxChipX());
		assertEquals(7, instance.maxChipY());

		for (var c : instance.chips()) {
			assertEquals(address, c.ipAddress);
			assertEquals(SDRAM, c.sdram);
			assert (c.router.links().containsAll(LINKS));
			for (var p : c.userProcessors()) {
				assertFalse(p.isMonitor);
			}
		}

		assertEquals(450, instance.totalCores());
		assertEquals(425, instance.totalAvailableUserCores());
		assertEquals(ChipLocation.ZERO_ZERO, instance.boot);
		assertEquals(address, instance.bootChip().ipAddress);
		assertEquals(25, instance.nChips());
		/*
		 * Not implemented as Java has no len and size() could be boards, chips,
		 * processors ect so a bad call anyway
		 */
		//self.assertEqual(len(new_machine), 25)
		/*
		 * Not implemented as Java has no iter and iter() could be boards,
		 * chips, processors ect so a bad call anyway
		 */
		//self.assertEqual(next(x[1].ip_address for x in new_machine), self._ip)
		assertEquals(ChipLocation.ZERO_ZERO,
				instance.chipCoordinates().iterator().next());
		// String is simplified to assumje each link unique and bi directional
		assertEquals("450 cores and 50.0 links",
				instance.coresAndLinkOutputString());
		assertEquals("[Machine: max_x=7, max_y=7, n_chips=25]",
				instance.toString());
		assertFalse(instance.spinnakerLinks().iterator().hasNext());
		int count = 0;
		ChipLocation previous = null;
		for (var found : instance) {
			count++;
			if (previous != null) {
				assertThat(previous, lessThan(found.asChipLocation()));
			}
			previous = found.asChipLocation();
		}
		assertEquals(25, count);
		var all = instance.chipsMap();
		assertEquals(25, all.size());
		assertFalse(instance.hasChipAt(null));

		HasChipLocation hasLocation = null;
		assertFalse(instance.hasChipAt(hasLocation));
		hasLocation = new CoreLocation(3, 3, 2);
		assertTrue(instance.hasChipAt(hasLocation));
	}

	@Test
	public void testRepeatChipInvalid() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		chips.add(new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER, SDRAM,
				null, BOOT_CHIP));
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			var instance =
					new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);
		});
	}

	@Test
	public void testAddChip() throws UnknownHostException {
		var processors = createProcessors();
		var instance = new Machine(new MachineDimensions(8, 8), List.of(),
				BOOT_CHIP);
		var chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
				SDRAM, null, BOOT_CHIP);
		instance.addChip(chip00);
		assertEquals(1, instance.nChips());
		var repeat = new Chip(ChipLocation.ZERO_ZERO, processors, ROUTER,
				SDRAM, null, BOOT_CHIP);
		assertThrows(IllegalArgumentException.class, () -> {
			instance.addChip(repeat);
		});
		var outOfRange1 = new Chip(new ChipLocation(5, 11), processors, ROUTER,
				SDRAM, null, BOOT_CHIP);
		assertThrows(IllegalArgumentException.class, () -> {
			instance.addChip(outOfRange1);
		});
		var outOfRange2 = new Chip(new ChipLocation(11, 5), processors, ROUTER,
				SDRAM, null, BOOT_CHIP);
		assertThrows(IllegalArgumentException.class, () -> {
			instance.addChip(outOfRange2);
		});
		assertEquals(1, instance.nChips());
	}

	@Test
	public void testLinks() {
		var processors = createProcessors();
		var instance = new Machine(new MachineDimensions(8, 8), List.of(),
				BOOT_CHIP);
		var link01 = new Link(CHIP00, Direction.NORTH, CHIP01);
		var link10 = new Link(CHIP00, Direction.EAST, CHIP10);

		var router = new Router(List.of(link01, link10));
		var chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, router,
				SDRAM, null, BOOT_CHIP);
		// Chip created but not added
		assertFalse(instance.hasChipAt(ChipLocation.ZERO_ZERO));
		assertFalse(
				instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.NORTH));

		instance.addChip(chip00);
		// Chip added
		assertTrue(instance.hasChipAt(ChipLocation.ZERO_ZERO));
		assertTrue(instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.NORTH));
		assertFalse(
				instance.hasLinkAt(ChipLocation.ZERO_ZERO, Direction.SOUTH));
		assertFalse(instance.hasLinkAt(ChipLocation.ZERO_ZERO, null));
	}

	@Test
	public void testRepeatAdd() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		var instance =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);
		assertThrows(IllegalArgumentException.class, () -> {
			instance.addChip(new Chip(ChipLocation.ZERO_ZERO, processors,
					ROUTER, SDRAM, null, BOOT_CHIP));
		});
	}

	@Test
	public void testChipAt() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		var instance =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);
		assertEquals(chips.get(0), instance.getChipAt(ChipLocation.ZERO_ZERO));
		assertNull(instance.getChipAt(10, 10));
		assertTrue(instance.hasChipAt(ChipLocation.ZERO_ZERO));
		assertFalse(instance.hasChipAt(10, 10));
	}

	@Test
	public void testReserveSystemProcessor() throws UnknownHostException {
		var processors = new ArrayList<Processor>();
		processors.add(Processor.factory(0, true));
		processors.add(Processor.factory(1, true));
		for (int i = 2; i < 18; i++) {
			processors.add(Processor.factory(i));
		}
		var chips = new ArrayList<Chip>();
		var chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, null, SDRAM,
				InetAddress.getByAddress(BYTES00), BOOT_CHIP);
		chips.add(chip00);
		var chip01 = new Chip(new ChipLocation(0, 1), processors, null, SDRAM,
				null, BOOT_CHIP);
		chips.add(chip01);
		var chip02 = new Chip(new ChipLocation(0, 2), Set.of(),
				null, SDRAM, null, BOOT_CHIP);
		chips.add(chip02);
		var instance =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);
		// Already 2 cores reserved.
		assertEquals(processors.size() - 2, instance.maximumUserCoresOnChip());
		assertEquals((processors.size() - 2) * 2,
				instance.totalAvailableUserCores());
		assertEquals(processors.size() * 2, instance.totalCores());
	}

	@Test
	public void testMachineGetChipsOnBoard() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		var instance =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);
		int count = 0;
		for (@SuppressWarnings("unused") var chip : instance.iterChipsOnBoard(
				chips.get(3))) {
			count++;
		}
		// Does not include 0.4 as it is not on the board
		assertEquals(24, count);
		var iterator = instance.iterChipsOnBoard(chips.get(3)).iterator();
		count = 0;
		while (true) {
			try {
				iterator.next();
				count++;
			} catch (NoSuchElementException ex) {
				break;
			}
		}
		// Does not include 0.4 as it is not on the board
		assertEquals(24, count);
	}

	@Test
	public void testGetChipOverLink() {
		var instance = new Machine(new MachineDimensions(24, 24), List.of(),
				BOOT_CHIP);
		var processors = createProcessors();
		var chip = new Chip(new ChipLocation(23, 23), processors, ROUTER, SDRAM,
				null, BOOT_CHIP);
		instance.addChip(chip);
		assertEquals(chip,
				instance.getChipOverLink(CHIP00, Direction.SOUTHWEST));
	}

	@Test
	public void testNormalizeWithWrapAround() {
		var instance = new Machine(new MachineDimensions(48, 24), List.of(),
				ChipLocation.ZERO_ZERO);
		assertEquals(new ChipLocation(24, 0),
				instance.normalizedLocation(24, 24));
		assertEquals(new ChipLocation(24, 1),
				instance.normalizedLocation(24, 25));
		assertEquals(new ChipLocation(24, 0),
				instance.normalizedLocation(new ChipLocation(24, 24)));
	}

	@Test
	public void testNormalizeWithWrapVertical() {
		var instance = new Machine(new MachineDimensions(40, 24), List.of(),
				ChipLocation.ZERO_ZERO);
		assertEquals(MachineVersion.TRIAD_WITH_VERTICAL_WRAP, instance.version);
		assertEquals(new ChipLocation(24, 0),
				instance.normalizedLocation(24, 24));
		assertEquals(new ChipLocation(24, 1),
				instance.normalizedLocation(24, 25));
	}

	@Test
	public void testNormalizeWithWrapHorizontal() {
		var instance = new Machine(new MachineDimensions(48, 16), List.of(),
				ChipLocation.ZERO_ZERO);
		assertEquals(MachineVersion.TRIAD_WITH_HORIZONTAL_WRAP,
				instance.version);
		assertEquals(new ChipLocation(4, 14),
				instance.normalizedLocation(52, 14));
	}

	@Test
	public void testNormalizeWithOutWrapAround() {
		var instance = new Machine(new MachineDimensions(52, 28), List.of(),
				ChipLocation.ZERO_ZERO);
		assertEquals(new ChipLocation(24, 24),
				instance.normalizedLocation(24, 24));
		assertEquals(new ChipLocation(24, 24),
				instance.normalizedLocation(24, 24));
	}

	@Test
	public void testEthernetChip() throws UnknownHostException {
		var processors = createProcessors();
		var chips = new ArrayList<Chip>();
		var chip00 = new Chip(ChipLocation.ZERO_ZERO, processors, null, SDRAM,
				InetAddress.getByAddress(BYTES00), BOOT_CHIP);
		chips.add(chip00);
		var chip84 = new Chip(new ChipLocation(8, 4), processors, null, SDRAM,
				InetAddress.getByAddress(BYTES84), BOOT_CHIP);
		chips.add(chip84);
		var chip01 = new Chip(new ChipLocation(0, 1), processors, null, SDRAM,
				null, BOOT_CHIP);
		chips.add(chip01);
		var instance =
				new Machine(new MachineDimensions(12, 12), chips, BOOT_CHIP);
		assertEquals(3, instance.nChips());
		assertThat(instance.ethernetConnectedChips(),
				containsInAnyOrder(chip00, chip84));

		var chip48 = new Chip(new ChipLocation(4, 8), processors, null, SDRAM,
				InetAddress.getByAddress(BYTES48), BOOT_CHIP);
		instance.addChip(chip48);
		var chip02 = new Chip(new ChipLocation(0, 2), processors, null, SDRAM,
				null, BOOT_CHIP);
		instance.addChip(chip02);
		assertEquals(5, instance.nChips());
		assertThat(instance.ethernetConnectedChips(),
				containsInAnyOrder(chip00, chip84, chip48));
	}

	@Test
	public void testHole() throws UnknownHostException {
		var geometry = SpiNNakerTriadGeometry.getSpinn5Geometry();

		var processors = createProcessors();
		var chips = new ArrayList<Chip>();
		var all = new ArrayList<>(geometry.singleBoard());
		for (var location : all) {
			var router = createRouter(location, all);
			if (location.equals(new ChipLocation(0, 0))) {
				chips.add(new Chip(location, processors, router, SDRAM,
						InetAddress.getByAddress(BYTES00), BOOT_CHIP));
			} else if (location.equals(new ChipLocation(3, 3))) {
				// Leave a hole
				continue;
			} else {
				chips.add(new Chip(location, processors, router, SDRAM, null,
						BOOT_CHIP));
			}
		}

		var instance =
				new Machine(new MachineDimensions(12, 12), chips, BOOT_CHIP);
		assertEquals(47, instance.nChips());
		assertEquals("846 cores and 117.0 links",
				instance.coresAndLinkOutputString());

		var abnormalChips = instance.findAbnormalChips();
		assertEquals(0, abnormalChips.size());

		var abnormalLinks = instance.findAbnormalLinks();
		assertEquals(6, abnormalLinks.size());

		var rebuilt = instance.rebuild(abnormalChips, abnormalLinks);
		assertEquals("846 cores and 114.0 links",
				rebuilt.coresAndLinkOutputString());
	}

	@Test
	public void testUnreachable() throws UnknownHostException {
		var geometry = SpiNNakerTriadGeometry.getSpinn5Geometry();

		var processors = createProcessors();
		var chips = new ArrayList<Chip>();
		var all = new ArrayList<>(geometry.singleBoard());
		for (var location : all) {
			if (location.equals(new ChipLocation(0, 0))) {
				chips.add(new Chip(location, processors,
						createRouter(location, all), SDRAM,
						InetAddress.getByAddress(BYTES00), BOOT_CHIP));
			} else if (location.equals(new ChipLocation(3, 3))) {
				chips.add(new Chip(location, processors, new Router(), SDRAM,
						null, BOOT_CHIP));
				// Leave a hole
			} else {
				chips.add(new Chip(location, processors,
						createRouter(location, all), SDRAM, null, BOOT_CHIP));
			}
		}

		var instance =
				new Machine(new MachineDimensions(12, 12), chips, BOOT_CHIP);
		assertEquals(48, instance.nChips());
		assertEquals("864 cores and 117.0 links",
				instance.coresAndLinkOutputString());

		var abnormalChips = instance.findAbnormalChips();
		assertThat(abnormalChips, contains(new ChipLocation(3, 3)));

		var abnormalLinks = instance.findAbnormalLinks();
		// 6 as it also has only the invers links from 3,3
		assertEquals(6, abnormalLinks.size());

		var rebuilt = instance.rebuild(abnormalChips, abnormalLinks);
		assertEquals("846 cores and 114.0 links",
				rebuilt.coresAndLinkOutputString());

		@SuppressWarnings("unused")
		var rebuilt2 = rebuilt.rebuild();
	}

	@Test
	public void testUnreachableIncomingChips() {
		var ignoreLinks =
				Map.of(new ChipLocation(2, 2), Set.of(NORTHEAST),
						new ChipLocation(2, 3), Set.of(EAST),
						new ChipLocation(3, 4), Set.of(SOUTH),
						new ChipLocation(4, 4), Set.of(SOUTHWEST),
						new ChipLocation(4, 3), Set.of(WEST),
						new ChipLocation(3, 2), Set.of(NORTH));

		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				null, null, ignoreLinks);
		var abnormal = instance.findAbnormalLinks();
		assertEquals(1, abnormal.size());
	}

	@Test
	public void testEquals() throws UnknownHostException {
		var processors = createProcessors();
		var chips = createdChips(processors);
		/* var address = */ InetAddress.getByAddress(BYTES);

		var instance1 =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);

		var instance2 =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);

		assertEquals(instance1, instance2);

		chips.remove(3);

		var missingChip =
				new Machine(new MachineDimensions(8, 8), chips, BOOT_CHIP);

		assertNotEquals(instance1, missingChip);
		assertNotNull(instance1.difference(missingChip));
	}

}
