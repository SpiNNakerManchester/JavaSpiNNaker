/*
 * Copyright (c) 2018-2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.bmp;

/**
 * A simple description of a BMP to talk to. Supports equality and being used as
 * a hash key.
 */
public final class BMPCoords {
	/** The ID of the cabinet that contains the frame that contains the BMPs. */
	private final int cabinet;

	/**
	 * The ID of the frame that contains the master BMP. Frames are contained
	 * within a cabinet.
	 */
	private final int frame;

	/**
	 * Create an instance.
	 *
	 * @param cabinet
	 *            The ID of the cabinet that contains the frame that contains
	 *            the BMPs.
	 * @param frame
	 *            The ID of the frame that contains the master BMP. Frames are
	 *            contained within a cabinet.
	 */
	public BMPCoords(int cabinet, int frame) {
		this.cabinet = cabinet;
		this.frame = frame;
	}

	public int getCabinet() {
		return cabinet;
	}

	public int getFrame() {
		return frame;
	}

	@Override
	public int hashCode() {
		return cabinet << 16 | frame;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BMPCoords) {
			BMPCoords b = (BMPCoords) o;
			return cabinet == b.cabinet && frame == b.frame;
		}
		return false;
	}
}
