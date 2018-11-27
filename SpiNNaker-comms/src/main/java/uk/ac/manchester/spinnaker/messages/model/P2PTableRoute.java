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

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * P2P Routing table routes.
 */
public enum P2PTableRoute {
	/** Toward the East chip. (x+1,y) */
	EAST(0b000),
	/** Toward the North East chip. (x+1,y+1) */
	NORTH_EAST(0b001),
	/** Toward the North chip. (x,y+1) */
	NORTH(0b010),
	/** Toward the West chip. (x-1,y) */
	WEST(0b011),
	/** Toward the South West chip. (x-1,y-1) */
	SOUTH_WEST(0b100),
	/** Toward the South chip. (x,y-1) */
	SOUTH(0b101),
	/** No route to this chip. */
	NONE(0b110),
	/** Route to the monitor on the current chip. */
	MONITOR(0b111);

	/** The SpiNNaker value. */
	public final int value;
	private static final Map<Integer, P2PTableRoute> MAP = new HashMap<>();
	static {
		for (P2PTableRoute r : values()) {
			MAP.put(r.value, r);
		}
	}

	P2PTableRoute(int value) {
		this.value = value;
	}

	/**
	 * Get a route descriptor from its encoded form.
	 *
	 * @param value
	 *            The encoded form.
	 * @return The route descriptor.
	 */
	public static P2PTableRoute get(int value) {
		return requireNonNull(MAP.get(value),
				"unknown P2P table route: " + value);
	}
}
