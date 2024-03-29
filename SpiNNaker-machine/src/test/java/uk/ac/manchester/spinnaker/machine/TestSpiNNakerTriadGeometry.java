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

import uk.ac.manchester.spinnaker.utils.Counter;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestSpiNNakerTriadGeometry {

	public TestSpiNNakerTriadGeometry() {
	}

	/**
	 * This table was copied from python (negated) which claims it was produced
	 * using the code in Rig.
	 */
	private final int[][][] localTable = {
		{{0, 0}, {1, 0}, {2, 0}, {3, 0}, {4, 0}, {1, 4},
			{2, 4}, {3, 4}, {4, 4}, {5, 4}, {6, 4}, {7, 4}},
		{{0, 1}, {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1},
			{2, 5}, {3, 5}, {4, 5}, {5, 5}, {6, 5}, {7, 5}},
		{{0, 2}, {1, 2}, {2, 2}, {3, 2}, {4, 2}, {5, 2},
			{6, 2}, {3, 6}, {4, 6}, {5, 6}, {6, 6}, {7, 6}},
		{{0, 3}, {1, 3}, {2, 3}, {3, 3}, {4, 3}, {5, 3},
			{6, 3}, {7, 3}, {4, 7}, {5, 7}, {6, 7}, {7, 7}},
		{{4, 0}, {1, 4}, {2, 4}, {3, 4}, {4, 4}, {5, 4},
			{6, 4}, {7, 4}, {0, 0}, {1, 0}, {2, 0}, {3, 0}},
		{{4, 1}, {5, 1}, {2, 5}, {3, 5}, {4, 5}, {5, 5},
			{6, 5}, {7, 5}, {0, 1}, {1, 1}, {2, 1}, {3, 1}},
		{{4, 2}, {5, 2}, {6, 2}, {3, 6}, {4, 6}, {5, 6},
			{6, 6}, {7, 6}, {0, 2}, {1, 2}, {2, 2}, {3, 2}},
		{{4, 3}, {5, 3}, {6, 3}, {7, 3}, {4, 7}, {5, 7},
			{6, 7}, {7, 7}, {0, 3}, {1, 3}, {2, 3}, {3, 3}},
		{{4, 4}, {5, 4}, {6, 4}, {7, 4}, {0, 0}, {1, 0},
			{2, 0}, {3, 0}, {4, 0}, {1, 4}, {2, 4}, {3, 4}},
		{{4, 5}, {5, 5}, {6, 5}, {7, 5}, {0, 1}, {1, 1},
			{2, 1}, {3, 1}, {4, 1}, {5, 1}, {2, 5}, {3, 5}},
		{{4, 6}, {5, 6}, {6, 6}, {7, 6}, {0, 2}, {1, 2},
			{2, 2}, {3, 2}, {4, 2}, {5, 2}, {6, 2}, {3, 6}},
		{{4, 7}, {5, 7}, {6, 7}, {7, 7}, {0, 3}, {1, 3},
			{2, 3}, {3, 3}, {4, 3}, {5, 3}, {6, 3}, {7, 3}}
	};

	/**
	 * Test of getEthernetChip method, of class SpiNNakerTriadGeometry.
	 */
	@Test
	public void testLocalChipCoordinate() {
		var instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
		for (int x = 0; x < 12; x++) {
			for (int y = 0; y < 12; y++) {
				// px, py = delta_table[y][x]
				var result =
						instance.getLocalChipCoordinate(new ChipLocation(x, y));
				assertEquals(localTable[y][x][0], result.getX());
				assertEquals(localTable[y][x][1], result.getY());
			}
		}

		assertEquals(new ChipLocation(0, 0),
				instance.getLocalChipCoordinate(new ChipLocation(32, 28)));
		assertEquals(new ChipLocation(1, 1),
				instance.getLocalChipCoordinate(new ChipLocation(33, 29)));
	}

	@Test
	public void testGetEthernetChip() {
		var instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(0, 0), 96, 60));
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(0, 3), 96, 60));
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(4, 7), 96, 60));
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(7, 7), 96, 60));
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(7, 3), 96, 60));
		assertEquals(new ChipLocation(0, 0),
				instance.getRootChip(new ChipLocation(4, 0), 96, 60));

		assertEquals(new ChipLocation(0, 0),
				instance.getLocalChipCoordinate(new ChipLocation(8, 4)));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(8, 4), 96, 60));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(8, 7), 96, 60));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(11, 10), 96, 60));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(11, 4), 96, 60));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(0, 4), 12, 12));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(0, 11), 12, 12));
		assertEquals(new ChipLocation(92, 4),
				instance.getRootChip(new ChipLocation(0, 11), 60, 96));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(3, 11), 12, 12));
		assertEquals(new ChipLocation(8, 4),
				instance.getRootChip(new ChipLocation(3, 7), 12, 12));

		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(4, 8), 96, 60));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(4, 11), 12, 12));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(11, 11), 12, 12));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(8, 8), 60, 96));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(8, 3), 12, 12));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(11, 3), 12, 12));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(11, 0), 12, 12));
		assertEquals(new ChipLocation(4, 8),
				instance.getRootChip(new ChipLocation(5, 0), 12, 12));
		assertEquals(new ChipLocation(4, 56),
				instance.getRootChip(new ChipLocation(5, 0), 60, 96));
	}

	private static final ChipLocation CHIP12_12 = new ChipLocation(12, 12);

	private static final ChipLocation CHIP0_12 = new ChipLocation(0, 12);

	private static final ChipLocation CHIP12_0 = new ChipLocation(12, 0);

	@Test
	public void testGetPotentialEthernetChips() {
		var instance = SpiNNakerTriadGeometry.getSpinn5Geometry();

		var ethers =
				instance.getPotentialRootChips(new MachineDimensions(2, 2));
		assertThat(ethers, contains(ChipLocation.ZERO_ZERO));

		ethers = instance.getPotentialRootChips(new MachineDimensions(8, 8));
		assertThat(ethers, contains(ChipLocation.ZERO_ZERO));

		var chip48 = new ChipLocation(4, 8);
		var chip84 = new ChipLocation(8, 4);

		ethers = instance.getPotentialRootChips(new MachineDimensions(12, 12));
		assertThat(ethers,
				containsInAnyOrder(ChipLocation.ZERO_ZERO, chip48, chip84));

		ethers = instance.getPotentialRootChips(new MachineDimensions(16, 16));
		assertThat(ethers,
				containsInAnyOrder(ChipLocation.ZERO_ZERO, chip48, chip84));

		ethers = instance.getPotentialRootChips(new MachineDimensions(20, 20));
		assertThat(ethers, containsInAnyOrder(ChipLocation.ZERO_ZERO, chip48,
				chip84, CHIP12_0, CHIP12_12, CHIP0_12));

		ethers = instance.getPotentialRootChips(new MachineDimensions(24, 24));
		assertThat(ethers, hasSize(12));

	}

	@Test
	public void testSingleBoard() {
		var instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
		int count = 0;
		for (var chip : instance.singleBoard()) {
			count += 1;
			assertEquals(ChipLocation.ZERO_ZERO,
					instance.getRootChip(chip, 12, 12));
		}
		assertEquals(48, count);
	}

	@Test
	public void testSingleBoardForEach() {
		var instance = SpiNNakerTriadGeometry.getSpinn5Geometry();
		final var count = new Counter();
		instance.singleBoardForEach(chip -> {
			count.increment();
			assertEquals(ChipLocation.ZERO_ZERO,
					instance.getRootChip(chip, 12, 12));
		});
		assertEquals(48, count.get());
	}

}
