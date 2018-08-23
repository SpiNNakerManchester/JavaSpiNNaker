/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.util.ArrayList;
import java.util.Collection;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
public class Tags {

    private final DefaultMap<Vertex, ArrayList<IPTag>> ipTagsByVertex =
            new DefaultMap<>(ArrayList::new);

    public Collection<IPTag> getIpTagsForVertex(Vertex vertex) {
        return ipTagsByVertex.get(vertex);
    }
}
