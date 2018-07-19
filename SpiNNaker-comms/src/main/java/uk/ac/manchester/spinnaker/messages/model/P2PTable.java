package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Math.min;
import static java.util.Collections.unmodifiableSet;
import static uk.ac.manchester.spinnaker.messages.model.P2PTableRoute.NONE;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/** Represents a P2P routing table read from the machine. */
public class P2PTable {
	private final Map<ChipLocation, P2PTableRoute> routes;
	/** The width of the machine that this table represents. */
	public final int width;
	/** The height of the machine that this table represents. */
	public final int height;

	public P2PTable(MachineDimensions dimensions, Collection<ByteBuffer> columnData) {
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
		Iterator<ByteBuffer> columns = columnData.iterator();
		for (int x = 0; columns.hasNext(); x++) {
			ByteBuffer data = columns.next();
			int y = 0;
			int pos = 0;
			while (y < height) {
				int next_word = data.getInt(pos * 4);
				pos++;
				int chunkSize = min(8, height - y);
				for (int entry = 0; entry < chunkSize; entry++) {
					P2PTableRoute route = P2PTableRoute
							.get((next_word >> (3 * entry)) & 0b111);
					if (route != null && route != NONE) {
						routes.put(new ChipLocation(x, y), route);
					}
					y++;
				}
			}
		}
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
