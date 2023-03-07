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
package uk.ac.manchester.spinnaker.messages.model;

import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

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

	private static final Map<Integer, P2PTableRoute> MAP =
			makeEnumBackingMap(values(), v -> v.value);

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
