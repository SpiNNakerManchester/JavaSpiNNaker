/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.download;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.front_end.download.request.Placement;
import uk.ac.manchester.spinnaker.front_end.download.request.Vertex;
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
        assertEquals(1612972372, fromJson.getBaseAddress());
    }

    @Test
    public void testPlacementJson() throws IOException {
        URL url = TestPlacements.class.getResource("/placement.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        Placement fromJson = mapper.readValue(url, Placement.class);
        assertEquals(2, fromJson.getY());
    }

    @Test
    public void testSimpleJson() throws IOException {
        URL url = TestPlacements.class.getResource("/simple.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        List<Placement> fromJson = mapper.readValue(url, new TypeReference<List<Placement>>(){});
        assertEquals(2, fromJson.size());
    }

}
