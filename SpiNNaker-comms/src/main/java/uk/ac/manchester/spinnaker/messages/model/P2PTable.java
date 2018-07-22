package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Math.min;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.model.P2PTableRoute.NONE;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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

	public P2PTable(MachineDimensions dimensions,
			Collection<ByteBuffer> columnData) {
		this.routes = new HashMap<>();
		this.width = dimensions.width;
		this.height = dimensions.height;
		parseColumnData(columnData);
	}

	public P2PTable(int width, int height, Collection<ByteBuffer> columnData) {
		this.routes = new HashMap<>();
		this.width = width;
		this.height = height;
		parseColumnData(columnData);
	}

	private void parseColumnData(Iterable<ByteBuffer> columnData) {
		int x = 0;
		for (ByteBuffer buffer : columnData) {
			IntBuffer data = buffer.asIntBuffer();
			int chipX = x++;
			for (int y = 0; y < height; y += 8) {
				extractRoutes(chipX, y, data.get());
			}
		}
	}

	private void extractRoutes(int chipX, int chipYBase, int word) {
		range(0, min(8, height - chipYBase)).forEach(y -> {
			P2PTableRoute route = P2PTableRoute.get((word >> (3 * y)) & 0b111);
			if (route != null && route != NONE) {
				routes.put(new ChipLocation(chipX, chipYBase + y), route);
			}
		});
	}

	/**
	 * Get the number of bytes to be read for each column of the table.
	 *
	 * @param height
	 *            The height of the machine
	 */
	public static int getNumColumnBytes(int height) {
		return ((height + 7) / 8) * 4;
	}

	/**
	 * Get the offset of the next column in the table from the P2P base address.
	 *
	 * @param column
	 *            The column to be read
	 */
	public static int getColumnOffset(int column) {
		return ((256 * column) / 8) * 4;
	}

	/** Get an iterable of (x, y) coordinates in the table */
	public Set<ChipLocation> getChips() {
		return unmodifiableSet(routes.keySet());
	}

	/**
	 * Determines if there is a route in the P2P table to the given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip to look up.
	 */
	public boolean isRoute(HasChipLocation chip) {
		P2PTableRoute r = routes.get(chip.asChipLocation());
		return r != null && r != NONE;
	}

	/**
	 * Get the route to follow from this chip to the given chip.
	 *
	 * @param chip
	 *            The coordinates of the chip to find the route to
	 */
	public P2PTableRoute getRoute(HasChipLocation chip) {
		P2PTableRoute r = routes.get(chip.asChipLocation());
		return r == null ? NONE : r;
	}
}
