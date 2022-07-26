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

import static java.util.Objects.isNull;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.HALF_SIZE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_X_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_Y_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_HEIGHT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_WIDTH;

import java.util.Objects;

/**
 * Known types of machine. Properly this should be an open set.
 *
 * @author Christian-B
 */
public enum MachineVersion {

	/** Original style 4 chip board. */
	TWO(2, 2, 2, true, false, true, true, false),
	/** Common style 4 chip board. */
	THREE(3, 2, 2, true, false, true, true, false),
	/** Original style 48 chip board. */
	FOUR(4, 8, 8, false, true, false, false, false),
	/** Single common style 48 chip board. */
	FIVE(5, 8, 8, false, true, false, false, false),
	/** Combination of 3 common style 48 chips boards in a Toroid. */
	THREE_BOARD(12, 12),
	/** Combination of 24 common style 48 chips boards. */
	TWENTYFOUR_BOARD(48, 24),
	/** Combination of 120 common style 48 chips boards. */
	ONE_TWENTY_BOARD(96, 60),
	/** Combination of 600 common style 48 chips boards. */
	SIX_HUNDRED_BOARD(240, 120),
	/** Assumed Combination of 1200 common style 48 chips boards. */
	ONE_THOUSAND_TWO_HUNDRED_BOARD(240, 240),
	/**
	 * Combination of multiple common style 48 chips boards but made up of
	 * Triads without wrap-arounds.
	 * <p>
	 * This is typically what spalloc provides when returning part of a larger
	 * machine.
	 */
	TRIAD_NO_WRAPAROUND(false, false, true),
	/**
	 * Unknown combination of common style 48 chips boards but made up of Triads
	 * with both wrap-arounds.
	 * <p>
	 * All known machine sizes with both wrap-arounds have unique
	 * {@code MachineVersion}s.
	 * <p>
	 * Note: While this is physically possible with standards boards there is no
	 * known case then such a machine would be obtained. The assumption that
	 * this version has wrap-arounds is purely based on the size.
	 */
	TRIAD_WITH_WRAPAROUND(true, true, true),
	/**
	 * Combination of multiple common style 48 chips boards but made up of
	 * Triads which only wrap-arounds on the Horizontal / X axis.
	 * <p>
	 * This is possible when spalloc returns a large part of a machine.
	 */
	TRIAD_WITH_HORIZONTAL_WRAP(true, false, true),
	/**
	 * Combination of multiple common style 48 chips boards but made up of
	 * Triads which only wrap-arounds on the Vertical / Y axis.
	 * <p>
	 * This is possible when spalloc returns a large part of a machine.
	 */
	TRIAD_WITH_VERTICAL_WRAP(false, true, true),
	/**
	 * Unexpected combination of common style 48 chips boards not in made up of
	 * standard Triads.
	 * <p>
	 * Note: While this is physically possible with standards boards there is no
	 * known case then such a machine would be obtained. The assumption that
	 * this version does not have wrap-arounds is purely based on the size.
	 */
	NONE_TRIAD_LARGE(false, false, false),
	/**
	 * Unexpected small board probably created by adding a virtual chip to a 2
	 * by 2 board.
	 */
	EXTENDED_SMALL(true, true, false);

	/**
	 * Python ID for this version or {@code null} if no matching ID in python.
	 */
	public final Integer id;

	/** Indicates if this machine has exactly 4 chips is a 2 by 2 layout. */
	public final boolean isFourChip;

	/** Indicates if this machine has exactly one 48 chip board. */
	public final boolean isFourtyeightChip;

	/**
	 * Indicates if this machine is expected to have wrap-arounds on the X axis.
	 */
	public final boolean horizontalWrap;

	/**
	 * Indicates if this machine is expected to have wrap-arounds on the Y axis.
	 */
	public final boolean verticalWrap;

	/**
	 * Indicates if this board is made up of triads, i.e., one or more groups of
	 * three boards in the typical layout.
	 */
	public final boolean isTriad;

	/**
	 * The only possible dimensions for this version or {@code null} if multiple
	 * sizes allowed.
	 */
	public final MachineDimensions machineDimensions;

	private static final int DEFAULT_HARDWARE_VERSION = 5;

	/**
	 * Main constructor.
	 *
	 * @param id
	 *            Python ID for this version or {@code null} if no matching ID
	 *            in python.
	 * @param dimensions
	 *            The only possible dimensions for this version or {@code null}
	 *            if multiple sizes allowed
	 * @param isFourChip
	 *            Indicates if this machine has exactly 4 chips is a 2 by 2
	 *            layout.
	 * @param isFourtyeightChip
	 *            Indicates if this machine has exactly one 48 chip board.
	 * @param horizontalWrap
	 *            Indicates if this machine is expected to have wrap-arounds.
	 * @param verticalWrap
	 *            Indicates if this machine is expected to have wrap-arounds.
	 * @param isTriad
	 *            Indicates if this board is made up of triads,
	 */
	MachineVersion(Integer id, MachineDimensions dimensions, boolean isFourChip,
			boolean isFourtyeightChip, boolean horizontalWrap,
			boolean verticalWrap, boolean isTriad) {
		this.id = id;
		this.horizontalWrap = horizontalWrap;
		this.verticalWrap = verticalWrap;
		this.machineDimensions = dimensions;
		this.isFourChip = isFourChip;
		this.isFourtyeightChip = isFourtyeightChip;
		this.isTriad = isTriad;
	}

	/**
	 * Multi-board constructor based purely on size.
	 *
	 * @param width
	 *            Number of columns in chips.
	 * @param height
	 *            Number of rows in chips.
	 */
	MachineVersion(int width, int height) {
		this(null, new MachineDimensions(width, height), false, false,
				(width % TRIAD_HEIGHT == 0), (height % TRIAD_WIDTH == 0),
				isTriad(width, height));
	}

	/**
	 * Single board constructor.
	 *
	 * @param id
	 *            Python ID for this version or {@code null} if no matching ID
	 *            in python.
	 * @param width
	 *            Number of columns in chips.
	 * @param height
	 *            Number of rows in chips.
	 * @param isFourChip
	 *            Indicates if this machine has exactly 4 chips is a 2 by 2
	 *            layout.
	 * @param isFortyEightChip
	 *            Indicates if this machine has exactly one 48 chip board.
	 * @param wrapAround
	 *            Indicates if this machine is expected to have wrap-arounds.
	 * @param isTriad
	 *            Indicates if this board is made up of triads.
	 */
	MachineVersion(Integer id, int width, int height, boolean isFourChip,
			boolean isFortyEightChip, boolean horizontalWrap,
			boolean verticalWrap, boolean isTriad) {
		this(id, new MachineDimensions(width, height), isFourChip,
				isFortyEightChip, horizontalWrap, verticalWrap, isTriad);
	}

	/**
	 * Unspecified size constructor, assumed to be multi-board.
	 *
	 * @param wrapAround
	 *            Indicates if this machine is expected to have wrap-arounds.
	 * @param isTriad
	 *            Indicates if this board is made up of triads,
	 */
	MachineVersion(boolean horizontalWrap, boolean verticalWrap,
			boolean isTriad) {
		this(null, null, false, false, horizontalWrap, verticalWrap, isTriad);
	}

	/**
	 * Converts a python board ID into a MachineVersion.
	 *
	 * @param id
	 *            Python board version.
	 * @return Machine version assuming just a single board.
	 * @throws IllegalArgumentException
	 *             If the ID doesn't correspond to a supported version.
	 */
	public static MachineVersion byId(Integer id) {
		for (MachineVersion possible : MachineVersion.values()) {
			if (Objects.equals(possible.id, id)) {
				return possible;
			}
		}
		throw new IllegalArgumentException("No Board version with id: " + id);
	}

	/**
	 * Calculate the machine version based on the size.
	 *
	 * @param width
	 *            The width of the machine to find the version for.
	 * @param height
	 *            The height of the machine to find the version for.
	 * @return A Board version.
	 * @throws IllegalArgumentException
	 *             if the size is not valid.
	 */
	public static MachineVersion bySize(int width, int height) {
		return bySize(new MachineDimensions(width, height));
	}

	/**
	 * Calculate the machine version based on the size.
	 *
	 * @param dimensions
	 *            The width and height of the machine to find the version for.
	 * @return A Board version.
	 * @throws IllegalArgumentException
	 *             if the size is not valid.
	 */
	public static MachineVersion bySize(MachineDimensions dimensions) {
		if (THREE.machineDimensions.equals(dimensions)) {
			return THREE;
		}
		if (FIVE.machineDimensions.equals(dimensions)) {
			return FIVE;
		}
		for (MachineVersion possible : MachineVersion.values()) {
			if (dimensions.equals(possible.machineDimensions)) {
				return possible;
			}
		}
		if ((dimensions.width % TRIAD_HEIGHT == 0)
				&& (dimensions.height % TRIAD_WIDTH == 0)) {
			return TRIAD_WITH_WRAPAROUND;
		}
		if (((dimensions.width - HALF_SIZE) % TRIAD_HEIGHT == 0)
				&& ((dimensions.height - HALF_SIZE) % TRIAD_WIDTH == 0)) {
			return TRIAD_NO_WRAPAROUND;
		}
		// Handle 4 chip board extended with virtual_chips
		if (dimensions.width < SIZE_X_OF_ONE_BOARD) {
			if (dimensions.height < SIZE_Y_OF_ONE_BOARD) {
				return EXTENDED_SMALL;
			}
		}
		/*
		 * Having eliminated a single 4 chip and 48 chip board as well as a
		 * three board toroid we need at least two board on top of each other or
		 * next to each other
		 */
		if (dimensions.width < SIZE_X_OF_ONE_BOARD * 2) {
			if (dimensions.height < SIZE_Y_OF_ONE_BOARD * 2) {
				throw new IllegalArgumentException(
						"Dimensions " + dimensions + "too small!");
			}
		}
		if ((dimensions.width % TRIAD_HEIGHT == 0)
				&& ((dimensions.height - HALF_SIZE) % TRIAD_WIDTH == 0)) {
			return TRIAD_WITH_HORIZONTAL_WRAP;
		}
		if (((dimensions.width - HALF_SIZE) % TRIAD_HEIGHT == 0)
				&& (dimensions.height % TRIAD_WIDTH == 0)) {
			return TRIAD_WITH_VERTICAL_WRAP;
		}
		if (dimensions.width % HALF_SIZE == 0
				&& dimensions.height % HALF_SIZE == 0) {
			return NONE_TRIAD_LARGE;
		}
		throw new IllegalArgumentException("Dimensions " + dimensions
				+ "not possible with current boards!");
	}

	private static boolean hasWrapArounds(int width, int height) {
		return (width % TRIAD_HEIGHT == 0) && (height % TRIAD_WIDTH == 0);
	}

	private static boolean isTriad(int width, int height) {
		return hasWrapArounds(width, height)
				|| (((width - HALF_SIZE) % TRIAD_HEIGHT == 0)
						&& ((height - HALF_SIZE) % TRIAD_WIDTH == 0));
	}

	/**
	 * Get the version number of the SpiNN boards in the hardware configuration.
	 *
	 * @return 2, 3, 4 or 5.
	 */
	public int hardwareVersion() {
		if (isNull(id)) {
			return DEFAULT_HARDWARE_VERSION;
		}
		return id;
	}
}
