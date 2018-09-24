/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import org.hamcrest.collection.IsMapContaining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author micro
 */
public class TestJobDescription {
    
    @Test
    void testOneArg() throws IOException {
        String json = "{\"job_id\":12345,"
                + "\"owner\":\"someone@manchester.ac.uk\","
                + "\"start_time\":536925243666607,"
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
    }
    
}
