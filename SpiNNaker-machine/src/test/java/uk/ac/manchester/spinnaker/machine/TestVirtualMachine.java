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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTH;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTH;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.BOTTOM;
import static uk.ac.manchester.spinnaker.machine.datalinks.FpgaId.TOP_RIGHT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestVirtualMachine {

	@Test
	public void testSmallBoardsMini() {
		@SuppressWarnings("unused")
		var instance = new VirtualMachine(new MachineDimensions(2, 2));
	}

	@Test
	public void testSmallBoards() {
		var instance = new VirtualMachine(new MachineDimensions(2, 2),
				Set.of(), Map.of(), Map.of());
		assertEquals(MachineVersion.THREE, instance.version);
		assertEquals(4, instance.chips().size());
		for (var chip : instance.chips()) {
			if (MachineDefaults.FOUR_CHIP_DOWN_LINKS
					.containsKey(chip.asChipLocation())) {
				var bad = MachineDefaults.FOUR_CHIP_DOWN_LINKS
						.get(chip.asChipLocation());
				for (var link : chip.router) {
					assertThat(bad, not(hasItems(link.sourceLinkDirection())));
				}
			}
		}

		var address00 = instance.bootChip().ipAddress;
		assertNotNull(address00);

		instance.addFpgaLinks();
		var fpgalinks = instance.getFpgaLinks().toList();
		assertEquals(0, fpgalinks.size());

		var empty = instance.spinnakerLinks();
		assertThat(empty, IsEmptyCollection.empty());

		instance.addSpinnakerLinks();
		var links = instance.spinnakerLinks();
		assertEquals(2, links.size());
		for (var link : links) {
			assertEquals(address00, link.boardAddress);
		}
	}

	@Test
	public void testBad() {
		assertThrows(Exception.class, () -> {
			@SuppressWarnings("unused")
			var instance =
					new VirtualMachine(new MachineDimensions(121, 120));
		});
	}

	@Test
	public void testSingleBoard() {
		var instance = new VirtualMachine(MachineVersion.FOUR);
		assertEquals(48, instance.chips().size());
		instance.addFpgaLinks();
		var address = instance.bootChip().ipAddress;

		assertEquals("864 cores and 120.0 links",
				instance.coresAndLinkOutputString());

		instance.addFpgaLinks();
		var link = instance.getFpgaLink(BOTTOM, 3, address);
		assertEquals(address, link.boardAddress);
		assertEquals(SOUTH, link.direction);
		assertEquals(BOTTOM, link.fpgaId);
		assertEquals(3, link.fpgaLinkId);

		var links = instance.getFpgaLinks().toList();
		assertEquals(3 * 16, links.size());
	}

	@Test
	public void test3Boards() {
		var instance = new VirtualMachine(MachineVersion.THREE_BOARD);
		assertEquals(3 * 48, instance.chips().size());
		assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());

		instance.addFpgaLinks();
		var links = instance.getFpgaLinks().toList();
		assertEquals(0, links.size());
	}

	@Test
	public void testNullIgnores() {
		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				null, null, null);
		assertEquals(3 * 48, instance.chips().size());
		assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
		assertEquals("2592 cores and 432.0 links",
				instance.coresAndLinkOutputString());
	}

	@Test
	public void testSpinnakerLinks() {
		var ignoreLinks = Map.of(//
				new ChipLocation(0, 0), EnumSet.of(SOUTHWEST),
				new ChipLocation(8, 4), EnumSet.of(SOUTHWEST),
				new ChipLocation(4, 8), EnumSet.of(SOUTHWEST));
		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				null, null, ignoreLinks);
		assertFalse(instance.hasLinkAt(new ChipLocation(0, 0), SOUTHWEST));
		assertEquals(3 * 48, instance.chips().size());
		assertEquals(3 * 48 * 17, instance.totalAvailableUserCores());
		// Only ignored in one direction so 1.5 less
		assertEquals("2592 cores and 430.5 links",
				instance.coresAndLinkOutputString());
		var empty = instance.spinnakerLinks();
		assertThat(empty, IsEmptyCollection.empty());
		instance.addSpinnakerLinks();
		var links = instance.spinnakerLinks();
		assertEquals(3, links.size());
		for (var link : links) {
			assertEquals(SOUTHWEST, link.direction);
			assertEquals(0, link.spinnakerLinkId);
			assertNotNull(link.boardAddress);
		}
		var address84 = instance.getChipAt(8, 4).ipAddress;
		assertNotNull(address84);
		var data84 = instance.getSpinnakerLink(0, address84);
		assertEquals(Direction.byId(4), data84.direction);
		assertEquals(address84, data84.boardAddress);
		assertEquals(0, data84.spinnakerLinkId);
		var address00 = instance.bootChip().ipAddress;
		var data00 = instance.getBootSpinnakerLink(0);
		assertEquals(address00, data00.boardAddress);
		var data00a = instance.getSpinnakerLink(0, address00);
		assertEquals(data00, data00a);
		var data00b = instance.getSpinnakerLink(0, null);
		assertEquals(data00, data00b);
	}

	@Test
	public void test3BoardWrappedWithFPGALinks() {
		// Make room for fpga links with two none fpga ignores as well
		// South is a fpg NE is not
		var ignoreLinks = Map.of(//
				new ChipLocation(0, 0), EnumSet.of(SOUTH, NORTHEAST),
				new ChipLocation(0, 3), EnumSet.of(WEST),
				new ChipLocation(7, 2), EnumSet.of(NORTH),
				// Middle of board so never fpga
				new ChipLocation(1, 1), EnumSet.of(NORTH));

		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				null, null, ignoreLinks);
		// Only ignored in one direction so 2.5 less
		assertEquals("2592 cores and 429.5 links",
				instance.coresAndLinkOutputString());
		assertFalse(instance.hasLinkAt(new ChipLocation(7, 2), NORTH));
		instance.addFpgaLinks();
		var links = instance.getFpgaLinks().toList();
		assertEquals(3, links.size());

	}

	@Test
	public void test3BoardNoWrap() throws UnknownHostException {
		var instance = new VirtualMachine(new MachineDimensions(16, 16),
				null, null, Map.of());
		assertEquals(3 * 48, instance.chips().size());

		instance.addFpgaLinks();
		var links = instance.getFpgaLinks().toList();
		// 16 links per fpga
		// each board has 2 fpga open (one connected to other board)
		// There are three boards
		assertEquals(16 * 2 * 3, links.size());

		// Never fpga at the bbc internter address
		var bbc = InetAddress.getByName("151.101.128.81");
		assertNull(instance.getFpgaLink(BOTTOM, 0, bbc));
		assertFalse(instance.getFpgaLinks(bbc).iterator().hasNext());

		var bootAddress = instance.bootChip().ipAddress;

		// Never any addresses on the top right of the boot board
		assertNull(instance.getFpgaLink(TOP_RIGHT, 0, bootAddress));

		var iterator = instance.getFpgaLinks(bootAddress).iterator();
		int count = 0;
		while (iterator.hasNext()) {
			count++;
			iterator.next();
		}
		assertEquals(16 * 2, count);
	}

	@Test
	public void testIgnoreCores() {
		var ignoreCores = Map.of(new ChipLocation(7, 7), Set.of(3, 5, 7));
		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				null, ignoreCores, null);
		assertEquals(3 * 48, instance.chips().size());
		var chip = instance.getChipAt(7, 7);
		assertEquals(14, chip.nUserProcessors());
		assertEquals(3 * 48 * 17 - 3, instance.totalAvailableUserCores());
	}

	@Test
	public void testIgnoreChips() {
		var ignoreChips =
				Set.of(new ChipLocation(4, 4), new ChipLocation(9, 10));
		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				ignoreChips, null, null);
		assertEquals(3 * 48 - 2, instance.chips().size());
	}

	@Test
	public void testIgnoreRootChips() {
		var ignoreChips = Set.of(new ChipLocation(8, 4));
		// Note future Machine may disallow a null ethernet chip
		var instance = new VirtualMachine(new MachineDimensions(12, 12),
				ignoreChips, null, null);
		// Note future VirtualMachines may ignore the whole board!
		assertEquals(3 * 48 - 1, instance.chips().size());
		var chip = instance.getChipAt(2, 9);
		assertEquals(new ChipLocation(8, 4),
				chip.nearestEthernet.asChipLocation());
		assertNull(instance.getChipAt(chip.nearestEthernet));
	}

	@Test
	public void test24Boards() {
		var instance = new VirtualMachine(MachineVersion.TWENTYFOUR_BOARD);
		assertEquals(24 * 48, instance.chips().size());
	}

	@Test
	public void test120Boards() {
		var instance = new VirtualMachine(MachineVersion.ONE_TWENTY_BOARD);
		assertEquals(120 * 48, instance.chips().size());
	}

	@Test
	public void test600Boards() {
		var instance = new VirtualMachine(MachineVersion.SIX_HUNDRED_BOARD);
		assertEquals(600 * 48, instance.chips().size());
	}

	@Test
	public void test1200Boards() {
		var instance = new VirtualMachine(
				MachineVersion.ONE_THOUSAND_TWO_HUNDRED_BOARD);
		assertEquals(1200 * 48, instance.chips().size());
	}

	@Test
	public void testBiggestWrapAround() {
		var instance = new VirtualMachine(new MachineDimensions(252, 252),
				Set.of(), Map.of(), Map.of());
		assertEquals(252 * 252, instance.chips().size());
		assertEquals(MachineVersion.TRIAD_WITH_WRAPAROUND, instance.version);
	}

	@Test
	public void testBiggestNoneWrapAround() {
		var instance = new VirtualMachine(new MachineDimensions(244, 244),
				Set.of(), Map.of(), Map.of());
		assertEquals(57600, instance.chips().size());
		assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND, instance.version);
	}

	@Test
	public void testBiggestWeird() {
		var instance = new VirtualMachine(new MachineDimensions(252, 248));
		assertEquals(60528, instance.chips().size());
		assertEquals(MachineVersion.NONE_TRIAD_LARGE, instance.version);
	}

}
