package uk.ac.manchester.spinnaker.spalloc.messages;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 * Describes a chip by its X,Y location.
 */
@JsonPropertyOrder({
		"x", "y"
})
@JsonFormat(shape = ARRAY)
public final class ChipLocationBean implements HasChipLocation {
	private int x;
	private int y;

	/**
	 * Create with default coordinates.
	 */
	public ChipLocationBean() {
	}

	/**
	 * Create with given coordinates.
	 *
	 * @param x
	 *            the X coordinate
	 * @param y
	 *            the Y coordinate
	 */
	public ChipLocationBean(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	@Override
	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof ChipLocationBean) {
			ChipLocationBean c = (ChipLocationBean) other;
			return x == c.x && y == c.y;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return 2 * x + 3 * y;
	}

	@Override
	public String toString() {
		return "Chip(" + x + "," + y + ")";
	}
}
