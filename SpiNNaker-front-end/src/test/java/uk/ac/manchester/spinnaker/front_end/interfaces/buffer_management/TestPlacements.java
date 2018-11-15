/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;

/**
 *
 * @author Christian-B
 */
public class TestPlacements {

    @Test
    public void testVertexJson() throws IOException {
        URL url = TestPlacements.class.getResource("/vertex.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        Vertex fromJson = mapper.readValue(url, Vertex.class);
        assertEquals(1612972372, fromJson.recordingRegionBaseAddress);
    }

    @Test
    public void testPlacementJson() throws IOException {
        URL url = TestPlacements.class.getResource("/placement.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        Placement fromJson = mapper.readValue(url, Placement.class);
        assertEquals(2, fromJson.y);
    }

    @Test
    public void testSimpleJson() throws IOException {
        URL url = TestPlacements.class.getResource("/simple.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        List<Placement> fromJson = mapper.readValue(url, new TypeReference<List<Placement>>(){});
        assertEquals(2, fromJson.size());
    }

}
