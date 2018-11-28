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

/** The SCP LED actions. */
public enum LEDAction {
	/** Toggle the LED status. */
	TOGGLE(1),
	/** Turn the LED off. */
	OFF(2),
	/** Turn the LED on. */
	ON(3);

	/** The SCAMP-encoded value. */
	public final byte value;
	LEDAction(int value) {
		this.value = (byte) value;
	}
}
