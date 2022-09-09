/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.admin;

import static uk.ac.manchester.spinnaker.utils.CollectionUtils.makeEnumBackingMap;

import java.util.Map;

import uk.ac.manchester.spinnaker.alloc.model.Direction;

/**
 * Enumeration of links from a SpiNNaker chip, as used in the old spalloc.
 * <p>
 * Note that the numbers chosen have two useful properties:
 *
 * <ul>
 * <li>The integer values assigned are chosen to match the numbers used to
 * identify the links in the low-level software API and hardware registers.
 * <li>The links are ordered consecutively in anticlockwise order meaning the
 * opposite link is {@code (link+3)%6}.
 * </ul>
 * Note that the new Spalloc uses a different notation for link directions!
 *
 * @see Direction
 * @author Donal Fellows
 */
public enum Link { // FIXME
	/** East. */
	east(Direction.SE),
	/** North-East. */
	northEast(Direction.E),
	/** North. */
	north(Direction.N),
	/** West. */
	west(Direction.NW),
	/** South-West. */
	southWest(Direction.W),
	/** South. */
	south(Direction.S);

	private static final Map<Direction, Link> MAP =
			makeEnumBackingMap(values(), v -> v.d);

	private final Direction d;

	Link(Direction d) {
		this.d = d;
	}

	static Link of(Direction direction) {
		return MAP.get(direction);
	}
}
