/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Prototype with minimum information needed.
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
class Monitor implements HasCoreLocation {

    /** The x value of the core this placement is on. */
    final int x;
    /** The y value of the core this placement is on. */
    final int y;
    /** The p value of the core this placement is on. */
    final int p;

    private final List<Placement> placements;

    /**
     * Constructor with minimum information needed.
     *
     * Could be called from an unmarsheller.
     *
     * @param x
     * @param y
     * @param p
     * @param placements
     */
    Monitor(@JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "p", required = true) int p,
            @JsonProperty(value = "placements", required = true)
                    List<Placement> placements) {
        this.x = x;
        this.y = y;
        this.p = p;
        this.placements = placements;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getP() {
        return p;
    }

    /**
     * @return the placements
     */
    public List<Placement> getPlacements() {
        return Collections.unmodifiableList(placements);
    }

}
