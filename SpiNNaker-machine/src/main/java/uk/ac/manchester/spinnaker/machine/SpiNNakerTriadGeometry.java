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

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static uk.ac.manchester.spinnaker.machine.ChipLocation.ZERO_ZERO;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.HALF_SIZE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_X_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.SIZE_Y_OF_ONE_BOARD;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_HEIGHT;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.TRIAD_WIDTH;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Geometry of a "triad" of SpiNNaker boards.
 * <p>
 * The geometry is defined by the arguments to the constructor; the standard
 * arrangement can be obtained from get_spinn5_geometry.
 * <p>
 * Note that the geometry defines what a Triad is in terms of the dimensions of
 * a triad and where the root chips occur in the triad.
 *
 * @see <a href=
 *      "https://github.com/SpiNNakerManchester/SpiNNMachine/blob/master/spinn_machine/chip.py">
 *      Python Version</a>
 *
 * @author Christian-B
 */
public final class SpiNNakerTriadGeometry {
	private static SpiNNakerTriadGeometry spinn5TriadGeometry = null;

	/** Height of a triad in chips. */
	public final int triadHeight;

	/** Width of a triad in chips. */
	public final int triadWidth;

	/** Bottom Left corner Chips. Typically the Ethernet Chip */
	private final List<ChipLocation> roots;

	private final Map<ChipLocation, ChipLocation> localChipCoordinates;

	private final Collection<ChipLocation> singleBoardCoordinates;

	private final float xCentre;

	private final float yCentre;

	/**
	 * Constructor is private to force reuse of statically held Object(s).
	 *
	 * @param triadHeight
	 *            Height of a triad in chips.
	 * @param triadWidth
	 *            Width of a triad in chips.
	 * @param roots
	 *            Bottom Left corner chips within the triad
	 * @param xCentre
	 *            Magic number to adjust X to find the nearest root.
	 * @param yCentre
	 *            Magic number to adjust Y to find the nearest root.
	 */
	private SpiNNakerTriadGeometry(int triadHeight, int triadWidth,
			List<ChipLocation> roots, float xCentre, float yCentre) {
		this.triadHeight = triadHeight;
		this.triadWidth = triadWidth;
		this.roots = roots;

		var calulationEthernets = new ArrayList<Location>();

		for (var root : roots) {
			calulationEthernets.add(new Location(root, 0, 0));
			// Add fictional roots that are less than a full triad away
			if (root.getX() > 0) {
				calulationEthernets.add(new Location(root, triadHeight, 0));
			}
			if (root.getY() > 0) {
				calulationEthernets.add(new Location(root, 0, triadWidth));
			}
		}
		this.xCentre = xCentre;
		this.yCentre = yCentre;

		var lcc = new HashMap<ChipLocation, ChipLocation>();
		var sbc = new LinkedHashSet<ChipLocation>();

		for (int x = 0; x < triadHeight; x++) {
			for (int y = 0; y < triadWidth; y++) {
				var bestCalc = locateNearestRoot(x, y, calulationEthernets);
				var key = new ChipLocation(x, y);
				lcc.put(key, new ChipLocation(x - bestCalc.x, y - bestCalc.y));
				if (bestCalc.x == 0 && bestCalc.y == 0) {
					sbc.add(key);
				}
			}
		}

		this.localChipCoordinates = unmodifiableMap(lcc);
		this.singleBoardCoordinates = unmodifiableSet(sbc);
	}

	/**
	 * Get the hexagonal metric distance of a point from the centre of the
	 * hexagon.
	 * <p>
	 * Computes the max of the magnitude of the dot products with the normal
	 * vectors for the hexagon sides. The normal vectors are (1,0), (0,1) and
	 * (1,-1); we don't need to be careful with the signs of the vectors because
	 * we're about to do abs() of them anyway.
	 *
	 * @param x
	 *            The x-coordinate of the chip to get the distance for
	 * @param y
	 *            The y-coordinate of the chip to get the distance for
	 * @param xCentre
	 *            The x-coordinate of the centre of the hexagon. Note that this
	 *            is the theoretical centre, it might not be an actual chip
	 * @param yCentre
	 *            The y-coordinate of the centre of the hexagon. Note that this
	 *            is the theoretical centre, it might not be an actual chip
	 * @return how far the chip is away from the centre of the hexagon
	 */
	private static float hexagonalMetricDistance(int x, int y, float xCentre,
			float yCentre) {
		float dx = x - xCentre;
		float dy = y - yCentre;
		return max(abs(dx), max(abs(dy), abs(dx - dy)));
	}

	/**
	 * Get the coordinate of the nearest root chip down and left from a given
	 * chip.
	 *
	 * @param x
	 *            Whole machine x part of chip location.
	 * @param y
	 *            Whole machine x part of chip location.
	 * @param roots
	 *            List of all the roots to check including fictitious negative
	 *            ones.
	 * @return The nearest root found hopefully on the same board.
	 */
	private Location locateNearestRoot(int x, int y, List<Location> roots) {
		/*
		 * Find the coordinates of the closest root chip by measuring the
		 * distance to the nominal centre of each board; the closest root is the
		 * one that is on the same board as the one the chip is closest to the
		 * centre of.
		 */
		Location bestCalc = null;
		float bestDistance = Float.MAX_VALUE;
		for (var ethernet : roots) {
			float calc = hexagonalMetricDistance(x, y,
					ethernet.x + xCentre, ethernet.y + yCentre);
			if (calc < bestDistance) {
				bestDistance = calc;
				bestCalc = ethernet;
			}
		}
		return bestCalc;
	}

	/**
	 * Finds the root Chip for the board this Chip is on.
	 * <p>
	 * Warning: parameter order is width, height to match python.
	 *
	 * @param chip
	 *            Location of Chip with X and Y expresses as whole machine
	 *            coordinates.
	 * @param width
	 *            The width of the machine to find the chips in.
	 * @param height
	 *            The height of the machine to find the chips in.
	 * @return The whole machine location of the Bottom Left Chip expressed as
	 *         whole machine coordinates.
	 */
	public ChipLocation getRootChip(HasChipLocation chip, int width,
			int height) {
		var adjusted = new ChipLocation(chip.getX() % triadHeight,
				chip.getY() % triadWidth);
		var localChip = localChipCoordinates.get(adjusted);

		return new ChipLocation(
				(chip.getX() - localChip.getX() + height) % height,
				(chip.getY() - localChip.getY() + width) % width);
	}

	/**
	 * Converts whole machine coordinates into local ones.
	 *
	 * @param chip
	 *            Location of Chip with X and Y expresses as whole machine
	 *            coordinates.
	 * @return The local coordinates of the Chip on board.
	 */
	public ChipLocation getLocalChipCoordinate(HasChipLocation chip) {
		if (chip.getX() < triadWidth && chip.getY() < triadHeight) {
			return localChipCoordinates.get(chip.asChipLocation());
		}

		int x = chip.getX() % triadWidth;
		int y = chip.getY() % triadHeight;
		var adjusted = new ChipLocation(x, y);
		return localChipCoordinates.get(adjusted);
	}

	/**
	 * Get the coordinates of bottom left chip on each board.
	 * <p>
	 * The bottom left Chip(s) are the ones with the local coordinates 0, 0.
	 * This is also typically the ethernet one.
	 * <p>
	 * No check is done to see if all the boards are present, nor if the root
	 * chip is present and active.
	 *
	 * @param dimensions
	 *            Size of the machine along the x and y axes in Chips.
	 * @return List of the root ChipLocation that would be there is all possible
	 *         boards in the width and height are present.
	 */
	public Set<ChipLocation> getPotentialRootChips(
			MachineDimensions dimensions) {
		var results = new LinkedHashSet<ChipLocation>();
		int maxWidth;
		int maxHeight;
		if (dimensions.width % triadWidth == 0
				&& dimensions.height % triadHeight == 0) {
			maxWidth = dimensions.width;
			maxHeight = dimensions.height;
		} else {
			maxWidth = dimensions.width - SIZE_X_OF_ONE_BOARD + 1;
			maxHeight = dimensions.height - SIZE_Y_OF_ONE_BOARD + 1;
			if (maxWidth < 0 || maxHeight < 0) {
				results.add(ZERO_ZERO);
				return results;
			}
		}
		for (var chip : roots) {
			for (int x = chip.getX(); x < maxWidth; x += triadWidth) {
				for (int y = chip.getY(); y < maxHeight; y += triadHeight) {
					results.add(new ChipLocation(x, y));
				}
			}
		}
		return results;
	}

	/**
	 * An Collection all the chips on a Single board with a root of 0, 0.
	 *
	 * @return An unmodifiable Collection of the Locations on one board.
	 */
	public Collection<ChipLocation> singleBoard() {
		return singleBoardCoordinates;
	}

	/**
	 * An Iterator all the chips on a Single board with a root of 0, 0.
	 *
	 * @return All the Locations on one board.
	 */
	public Iterator<ChipLocation> singleBoardIterator() {
		return singleBoard().iterator();
	}

	/**
	 * Allows forEach to be called on all ChipLocations in
	 * SingleBoardCoordinates.
	 *
	 * @param action
	 *            The action to be performed for each element
	 */
	public void singleBoardForEach(Consumer<ChipLocation> action) {
		singleBoardCoordinates.forEach(action);
	}

	/**
	 * Get the geometry object for a SpiNN-5 arrangement of boards.
	 * <p>
	 * Note the centres are slightly offset so as to force which edges are
	 * included where
	 *
	 * @return SpiNN5 geometry
	 */
	public static SpiNNakerTriadGeometry getSpinn5Geometry() {
		if (spinn5TriadGeometry == null) {
			var roots = List.of(
					new ChipLocation(0, 0),
					new ChipLocation(HALF_SIZE, SIZE_Y_OF_ONE_BOARD),
					new ChipLocation(SIZE_X_OF_ONE_BOARD, HALF_SIZE));

			spinn5TriadGeometry = new SpiNNakerTriadGeometry(TRIAD_HEIGHT,
					TRIAD_WIDTH, roots, VIRTUAL_CENTRE_X, VIRTUAL_CENTRE_Y);
		}
		return spinn5TriadGeometry;
	}

	private static final float VIRTUAL_CENTRE_X = 3.6F;

	private static final float VIRTUAL_CENTRE_Y = 3.4F;

	/**
	 * Local class with x and y values that can be negative.
	 */
	private static final class Location {
		final int x;

		final int y;

		Location(ChipLocation chip, int dx, int dy) {
			this.x = chip.getX() - dx;
			this.y = chip.getY() - dy;
		}

		@Override
		public String toString() {
			return ("(" + x + ", " + y + ")");
		}
	}
}
