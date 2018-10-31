/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

/**
 *
 * @author Christian-B
 */
@JsonFormat(shape = ARRAY)
public class Placements {

    private final LinkedHashMap<Vertex, Placement> machineVertices;

    public Placements(@JsonProperty(value = "placements", required = true) List<Placement> placements){
        machineVertices = new LinkedHashMap<>();
    }

    public void addPlacement(Placement placement){
        machineVertices.put(placement.vertex, placement);
    }

    Placement getPlacementOfVertex (Vertex vertex) {
        if (machineVertices.containsKey(vertex)) {
            return machineVertices.get(vertex);
        }
        throw new IllegalArgumentException("No placement found for " + vertex);
    }

    public Collection<Placement> getPlacements() {
        return machineVertices.values();
    }
}
