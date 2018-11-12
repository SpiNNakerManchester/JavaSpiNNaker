/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 *
 * @author Christian-B
 */
class Placement implements HasCoreLocation {

    final int x;
    final int y;
    final int p;
    final Vertex vertex;

    public Placement(int x, int y, int p, Vertex vertex) {
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
