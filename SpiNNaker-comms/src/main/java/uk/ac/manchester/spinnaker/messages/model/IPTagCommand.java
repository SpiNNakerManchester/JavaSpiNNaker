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

/** SCP IP tag Commands. */
public enum IPTagCommand {
	/** Create. */
	NEW(0),
	/** Update. */
	SET(1),
	/** Fetch. */
	GET(2),
	/** Delete. */
	CLR(3),
	/** Update Meta. */
	TTO(4);

	/** The SCAMP-encoded value. */
	public final byte value;
	IPTagCommand(int value) {
		this.value = (byte) value;
	}
}
