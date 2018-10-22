/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.util.LinkedHashMap;

/**
 *
 * @author Christian-B
 */
public class Placements {

    private final LinkedHashMap<Vertex, Placement> machineVertices =
        new LinkedHashMap<>();

    Placement getPlacementOfVertex (Vertex vertex) {
        if (machineVertices.containsKey(vertex)) {
            return machineVertices.get(vertex);
        }
        throw new IllegalArgumentException("No placement found for " + vertex);
    }
}
