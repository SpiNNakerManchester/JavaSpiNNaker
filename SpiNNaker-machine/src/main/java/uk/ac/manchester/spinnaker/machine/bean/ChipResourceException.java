/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Christian-B
 */
@JsonPropertyOrder({
		"x", "y", "exceptions"
})
@JsonFormat(shape = ARRAY)

public class ChipResourceException {
	private int x;
	private int y;
    private ChipResources exceptions;

    /**
     * @return the exceptions
     */
    public ChipResources getExceptions() {
        return exceptions;
    }

    /**
     * @param exceptions the exceptions to set
     */
    public void setExceptions(ChipResources exceptions) {
        this.exceptions = exceptions;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(int x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(int y) {
        this.y = y;
    }

    @Override
	public String toString() {
		return "(" + x + ", " + y + ", " + exceptions +")";
	}

}
