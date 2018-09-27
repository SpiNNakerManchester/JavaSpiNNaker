package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The logical coordinates of a board.
 */
@JsonPropertyOrder({
		"x", "y", "z"
})
@JsonFormat(shape = ARRAY)
public final class BoardCoordinates {
	private int x, y, z;

	/**
	 * Create with default coordinates.
	 */
	public BoardCoordinates() {
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param x
	 *            the X coordinate
	 * @param y
	 *            the Y coordinate
	 * @param z
	 *            the Z coordinate
	 */
	public BoardCoordinates(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof BoardCoordinates) {
			BoardCoordinates other = (BoardCoordinates) o;
			return x == other.x && y == other.y && z == other.z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return x * 1234567 + y * 56789 + z;
	}

	@Override
	public String toString() {
		return "Board@(" + x + "," + y + "," + z + ")";
	}
}
