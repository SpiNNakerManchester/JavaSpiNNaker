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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Map;

import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestJobDescription {

    @Test
    void testOneArg() throws IOException {
        String json = "{\"job_id\":12345,"
                + "\"owner\":\"someone@manchester.ac.uk\","
                + "\"start_time\":1.537284307847865E9,"
                + "\"keepalive\":45.0,"
                + "\"state\":3,"
                + "\"power\":true,"
                + "\"args\":[1],"
                + "\"kwargs\":{\"tags\":null,"
                    + "\"max_dead_boards\":0,"
                    + "\"machine\":null,"
                    + "\"min_ratio\":0.333,"
                    + "\"max_dead_links\":null,"
                    + "\"require_torus\":false},"
                + "\"allocated_machine_name\":\"Spin24b-223\","
                + "\"boards\":[[1,1,2]],"
                + "\"keepalivehost\":\"130.88.198.171\"}";

        ObjectMapper mapper = SpallocClient.createMapper();
        JobDescription fromJson = mapper.readValue(json, JobDescription.class);
        assertEquals(12345, fromJson.getJobID());
        assertEquals("someone@manchester.ac.uk", fromJson.getOwner());
        assertEquals(1.537284307847865E9, fromJson.getStartTime());
        assertEquals(45, fromJson.getKeepAlive());
        assertEquals(State.values()[3], fromJson.getState());
        assertEquals(true, fromJson.getPower());
        assertThat(fromJson.getArgs(), contains(1));
        Map<String, Object> map = fromJson.getKwargs();
        assertThat(map, IsMapContaining.hasEntry("tags", null));
        assertThat(map, IsMapContaining.hasEntry("max_dead_boards", 0));
        assertThat(map, IsMapContaining.hasEntry("machine", null));
        assertThat(map, IsMapContaining.hasEntry("min_ratio", 0.333));
        assertThat(map, IsMapContaining.hasEntry("max_dead_links", null));
        assertThat(map, IsMapContaining.hasEntry("require_torus", false));
        assertEquals("Spin24b-223", fromJson.getMachine());
        assertThat(fromJson.getBoards(), contains(new BoardCoordinates(1,1,2)));
        assertEquals("130.88.198.171", fromJson.getKeepAliveHost());
    }

    @Test
    void testNulls() throws IOException {
        String json = "{\"job_id\":null}";

        ObjectMapper mapper = SpallocClient.createMapper();
        JobDescription fromJson = mapper.readValue(json, JobDescription.class);
        assertEquals(0, fromJson.getJobID());
        assertNotNull(fromJson.toString());
        /*
        assertEquals("someone@manchester.ac.uk", fromJson.getOwner());
        assertEquals(536925243666607l, fromJson.getStartTime());
        assertEquals(45, fromJson.getKeepAlive());
        assertEquals(State.values()[3], fromJson.getState());
        assertEquals(true, fromJson.getPower());
        assertThat(fromJson.getArgs(), contains(1));
        Map<String, Object> map = fromJson.getKwargs();
        assertThat(map, IsMapContaining.hasEntry("tags", null));
        assertThat(map, IsMapContaining.hasEntry("max_dead_boards", 0));
        assertThat(map, IsMapContaining.hasEntry("machine", null));
        assertThat(map, IsMapContaining.hasEntry("min_ratio", 0.333));
        assertThat(map, IsMapContaining.hasEntry("max_dead_links", null));
        assertThat(map, IsMapContaining.hasEntry("require_torus", false));
        assertEquals("Spin24b-223", fromJson.getMachine());
        assertThat(fromJson.getBoards(), contains(new BoardCoordinates(1,1,2)));
        assertEquals("130.88.198.171", fromJson.getKeepAliveHost());
*/
    }
}
