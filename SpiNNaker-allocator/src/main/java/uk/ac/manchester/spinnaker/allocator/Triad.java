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
package uk.ac.manchester.spinnaker.allocator;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NON_PRIVATE;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;

import uk.ac.manchester.spinnaker.machine.board.ValidTriadX;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadY;
import uk.ac.manchester.spinnaker.machine.board.ValidTriadZ;

/** Logical coordinates of a board. */
@JsonFormat(shape = ARRAY)
@JsonAutoDetect(setterVisibility = NON_PRIVATE)
public class Triad {
	@ValidTriadX
	private int x;

	@ValidTriadY
	private int y;

	@ValidTriadZ
	private int z;

	Triad() {
	}

	/**
	 * @param x The X coordinate of the board.
	 * @param y The Y coordinate of the board.
	 * @param z The Z coordinate of the board.
	 */
	public Triad(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** @return The X coordinate of the board. */
	public int getX() {
		return x;
	}

	void setX(int x) {
		this.x = x;
	}

	/** @return The Y coordinate of the board. */
	public int getY() {
		return y;
	}

	void setY(int y) {
		this.y = y;
	}

	/** @return The Z coordinate of the board. */
	public int getZ() {
		return z;
	}

	void setZ(int z) {
		this.z = z;
	}

	@Override
	public String toString() {
		return format("[X:%d, Y:%d, Z:%d]", x, y, z);
	}
}
