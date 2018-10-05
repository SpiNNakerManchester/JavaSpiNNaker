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

/**
 *
 * @author Christian-B
 */
@JsonPropertyOrder({
		"x", "y", "exceptions"
})
@JsonFormat(shape = ARRAY)

public class ChipResourceException {
	public final ChipLocation location;
    public final ChipResources exceptions;

   /**
     * Create the location of a chip on a SpiNNaker machine.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param exceptions The Exceptions for this Chip.
     * @throws IllegalArgumentException
     *      Thrown is either x or y is negative or too big.
     */
    @JsonCreator
    public ChipResourceException(
            @JsonProperty(value = "x", required=true) int x,
            @JsonProperty(value = "y", required=true) int y,
            @JsonProperty(value = "exceptions", required=true)
                    ChipResources exceptions) {
        location = new ChipLocation(x, y);
        this.exceptions = exceptions;
    }

    /**
     * @return the exceptions
     */
    public ChipResources getExceptions() {
        return exceptions;
    }

    public ChipLocation getLocation() {
        return location;
    }

    @Override
	public String toString() {
		return location+  ": " + exceptions;
	}

}
