/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.OBJECT;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 *
 * @author Christian-B
 */
@JsonFormat(shape = OBJECT)
class Placement implements HasCoreLocation {

    final int x;
    final int y;
    final int p;
    final Vertex vertex;

    public Placement(@JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "p", required = true) int p,
            @JsonProperty(value = "vertex", required = true) Vertex vertex) {
        this.x = x;
        this.y = y;
        this.p = p;
        this.vertex = vertex;
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

    Vertex getVertex() {
        return vertex;
    }

}
