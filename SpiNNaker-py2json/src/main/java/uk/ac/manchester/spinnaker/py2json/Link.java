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
package uk.ac.manchester.spinnaker.py2json;

/**
 * Enumeration of links from a SpiNNaker chip.
 * <p>
 * Note that the numbers chosen have two useful properties:
 *
 * <ul>
 * <li>The integer values assigned are chosen to match the numbers used to
 * identify the links in the low-level software API and hardware registers.
 * <li>The links are ordered consecutively in anticlockwise order meaning the
 * opposite link is {@code (link+3)%6}.
 * </ul>
 */
public enum Link {
	/** East. */
	east,
	/** North-East. */
	northEast,
	/** North. */
	north,
	/** West. */
	west,
	/** South-West. */
	southWest,
	/** South. */
	south
}
