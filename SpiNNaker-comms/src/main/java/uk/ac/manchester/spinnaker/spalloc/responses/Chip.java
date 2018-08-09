package uk.ac.manchester.spinnaker.spalloc.responses;

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
public class Chip implements HasChipLocation {
	private int x;
	private int y;

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
}
