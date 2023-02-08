/*
 * Copyright (c) 2018 The University of Manchester
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
package uk.ac.manchester.spinnaker.machine;

import static uk.ac.manchester.spinnaker.machine.Direction.EAST;
import static uk.ac.manchester.spinnaker.machine.Direction.NORTHEAST;
import static uk.ac.manchester.spinnaker.machine.Direction.SOUTHWEST;
import static uk.ac.manchester.spinnaker.machine.Direction.WEST;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGAHERTZ_PER_HERTZ;

import java.util.EnumSet;
import java.util.Map;

/**
 * Default values for a SpiNNaker machine comprised of SpiNNaker-1 chips running
 * on boards of version 2 through 5.
 *
 * @author Christian-B
 */
public final class MachineDefaults {
	private MachineDefaults() {
	}

	/** Default SDRAM per chip after SCAMP has reserved space for itself. */
	public static final int SDRAM_PER_CHIP = 117 * 1024 * 1024;

	/** Clock speed in MHz of a standard Processor. */
	public static final int PROCESSOR_CLOCK_SPEED = 200 * MEGAHERTZ_PER_HERTZ;

	/** DTCM available on each standard Processor. */
	public static final int DTCM_AVAILABLE = 65536; // 2 ** 16;

	/** Standard number of Processors on each Chip. */
	public static final int PROCESSORS_PER_CHIP = 18;

	/** Entries available on a standard Router. */
	public static final int ROUTER_AVAILABLE_ENTRIES = 1024;

	/** Maximum links available on a standard Router. */
	public static final int MAX_LINKS_PER_ROUTER = 6;

	/** Maximum X coordinate for a chip regardless of the type of machine. */
	public static final int MAX_X = 255;

	/** Maximum Y coordinate for a chip regardless of the type of machine. */
	public static final int MAX_Y = 255;

	/** The number of rows of chips on each 48 Chip board. */
	public static final int SIZE_X_OF_ONE_BOARD = 8;

	/** The number of columns of chips on each 48 Chip board. */
	public static final int SIZE_Y_OF_ONE_BOARD = 8;

	/**
	 * The height of only known Triad in chips. Spalloc arranges boards in
	 * groups of three &mdash; triads &mdash; that tile out to form a large
	 * machine.
	 */
	public static final int TRIAD_HEIGHT = 12;

	/**
	 * The width of the Triad in chips. Spalloc arranges boards in groups of
	 * three &mdash; triads &mdash; that tile out to form a large machine.
	 */
	public static final int TRIAD_WIDTH = 12;

	/** The offset from zero in chips to get half size root values. */
	public static final int HALF_SIZE = 4;

	/**
	 * The number of router diagnostic counters.
	 */
	public static final int NUM_ROUTER_DIAGNOSTIC_COUNTERS = 16;

	/**
	 * Width of field of hashcode for holding (one dimension of the) chip
	 * coordinate.
	 */
	public static final int COORD_SHIFT = 8;

	/** The maximum number of cores present on a chip. */
	public static final int MAX_NUM_CORES = 18;

	/** Width of field of hashcode for holding processor ID. */
	public static final int CORE_SHIFT = 5;

	/** Width of field of hashcode for holding region ID. */
	public static final int REGION_SHIFT = 4;

	/** Ignore Links info for a four chip board. */
	// _4_chip_down_links = {
	// (0, 0, 3), (0, 0, 4), (0, 1, 3), (0, 1, 4),
	// (1, 0, 0), (1, 0, 1), (1, 1, 0), (1, 1, 1)
	// }
	public static final Map<ChipLocation,
			EnumSet<Direction>> FOUR_CHIP_DOWN_LINKS = Map.of(
					new ChipLocation(0, 0), EnumSet.of(WEST, SOUTHWEST),
					new ChipLocation(0, 1), EnumSet.of(WEST, SOUTHWEST),
					new ChipLocation(1, 0), EnumSet.of(EAST, NORTHEAST),
					new ChipLocation(1, 1), EnumSet.of(EAST, NORTHEAST));

	/**
	 * Checks the x and y parameter are legal ones regardless of the type of
	 * machine.
	 *
	 * @param x
	 *            X part of the chips location
	 * @param y
	 *            Y part of the chips location
	 * @throws IllegalArgumentException
	 *             Thrown is either x or y is negative or too big.
	 */
	public static void validateChipLocation(int x, int y)
			throws IllegalArgumentException {
		if (x < 0 || x > MAX_X) {
			throw new IllegalArgumentException("bad X co-ordinate: " + x);
		}
		if (y < 0 || y > MAX_Y) {
			throw new IllegalArgumentException("bad Y co-ordinate: " + y);
		}
	}

	/**
	 * Checks the x, y and p, parameter are legal ones regardless of the type of
	 * machine.
	 *
	 * @param x
	 *            X part of the core/chip's location
	 * @param y
	 *            Y part of the core/chip's location
	 * @param p
	 *            P part of the core's location
	 * @throws IllegalArgumentException
	 *             Thrown is x, y or p are negative or too big.
	 */
	public static void validateCoreLocation(int x, int y, int p)
			throws IllegalArgumentException {
		validateChipLocation(x, y);
		if (p < 0 || p >= MAX_NUM_CORES) {
			throw new IllegalArgumentException("bad processor ID: " + p);
		}
	}
}
