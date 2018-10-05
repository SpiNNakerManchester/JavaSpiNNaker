/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;

/**
 *
 * @author Christian-B
 */
@JsonPropertyOrder({
		"x", "y", "direction"
})
@JsonFormat(shape = ARRAY)
public class DeadLink {
	public final ChipLocation location;
    public final Direction direction;

    @JsonCreator
    public DeadLink(
            @JsonProperty(value = "x", required=true) int x,
            @JsonProperty(value = "y", required=true) int y,
            @JsonProperty(value = "direction", required=true)
                    String direction) {
        location = new ChipLocation(x, y);
        this.direction = Direction.byLabel(direction);
    }

    public ChipLocation getLocation() {
        return location;
    }

    /**
     * @return the direction
     */
    public Direction getDirection() {
        return direction;
    }

    public String toString() {
		return location + ", " + direction;
    }
}
