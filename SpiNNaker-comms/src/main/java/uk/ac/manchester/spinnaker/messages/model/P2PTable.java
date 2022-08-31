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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Math.min;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import static uk.ac.manchester.spinnaker.messages.model.P2PTableRoute.NONE;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;

/** Represents a P2P routing table read from the machine. */
public class P2PTable {
	private final Map<ChipLocation, P2PTableRoute> routes;

	/** The width of the machine that this table represents. */
	public final int width;

	/** The height of the machine that this table represents. */
	public final int height;

	private static final int ROUTE_CHUNK = 8;

	private static final int COLUMN_SIZE = 256;

	private static final int ROUTE_BITS = 3;

	private static final int ROUTE_MASK = (1 << ROUTE_BITS) - 1;

	/** Number of bits per byte. */
	private static final int NBBY = 8;

	/**
	 * Construct a routing table from data.
	 *
	 * @param dimensions
	 *            The size of the machine.
	 * @param columnData
	 *            The routing data to parse.
	 */
	public P2PTable(MachineDimensions dimensions,
			Collection<ByteBuffer> columnData) {
		this.routes = new HashMap<>();
		this.width = dimensions.width;
		this.height = dimensions.height;
		parseColumnData(columnData);
	}

	private void parseColumnData(Iterable<ByteBuffer> columnData) {
		int x = 0;
		for (var buffer : columnData) {
			var data = buffer.asIntBuffer();
			int chipX = x++;
			for (int y = 0; y < height; y += ROUTE_CHUNK) {
				extractRoutes(chipX, y, data.get());
			}
		}
	}

	private void extractRoutes(int chipX, int chipYBase, int word) {
		range(0, min(ROUTE_CHUNK, height - chipYBase)).forEach(y -> {
			var route =
					P2PTableRoute.get((word >> (ROUTE_BITS * y)) & ROUTE_MASK);
			if (nonNull(route) && route != NONE) {
				routes.put(new ChipLocation(chipX, chipYBase + y), route);
			}
		});
	}

	/**
	 * Get the number of bytes to be read for each column of the table.
	 *
	 * @param height
	 *            The height of the machine
	 * @return The number of bytes for the column
	 */
	public static int getNumColumnBytes(int height) {
		return ((height + NBBY - 1) / NBBY) * WORD_SIZE;
	}

	/**
	 * Get the offset of the next column in the table from the P2P base address.
	 *
	 * @param column
	 *            The column to be read
	 * @return Where the column is located within the table.
	 */
	public static int getColumnOffset(int column) {
		return ((COLUMN_SIZE * column) / NBBY) * WORD_SIZE;
	}

	/** @return The coordinates of chips in the table. */
	public Set<ChipLocation> getChips() {
		return unmodifiableSet(routes.keySet());
	}

	/**
	 * Determines if there is a route in the P2P table to the given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip to look up.
	 * @return True if anything in the table routes to the chip.
	 */
	public boolean isRoute(HasChipLocation chip) {
		var r = routes.get(chip.asChipLocation());
		return nonNull(r) && r != NONE;
	}

	/**
	 * Get the route to follow from this chip to the given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip to find the route to
	 * @return Get the route descriptor for a chip.
	 */
	public P2PTableRoute getRoute(HasChipLocation chip) {
		var r = routes.get(chip.asChipLocation());
		return isNull(r) ? NONE : r;
	}
}
