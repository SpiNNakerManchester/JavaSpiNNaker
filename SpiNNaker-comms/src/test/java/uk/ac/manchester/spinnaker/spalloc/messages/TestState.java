package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestState {
    
    @Test
    void testFromJson() throws IOException {
        String json = "{\"state\":2,"
            + "\"power\":true,"
            + "\"keepalive\":60.0,"
            + "\"reason\":null,"
            + "\"start_time\":1.537284307847865E9,"
            + "\"keepalivehost\":\"86.82.216.229\"}";
        ObjectMapper mapper = SpallocClient.createMapper();
        JobState fromJson = mapper.readValue(json, JobState.class);
        assertEquals(State.POWER, fromJson.getState());
        assertEquals(true, fromJson.getPower());
        assertEquals(1537284307.847865f, fromJson.getStartTime());
        assertEquals(60, fromJson.getKeepalive());
        assertNull(fromJson.getReason());
        assertEquals("86.82.216.229", fromJson.getKeepalivehost());        
        assertNotNull(fromJson.toString());
    }

    @Test
    void testNullJson() throws IOException {
        String json = "{\"reason\":null}";
        ObjectMapper mapper = SpallocClient.createMapper();
        JobState fromJson = mapper.readValue(json, JobState.class);
        assertNull(fromJson.getState());
        assertNull(fromJson.getPower());
        assertNull(fromJson.getReason());
        assertEquals(0.0, fromJson.getStartTime());
        assertNull(fromJson.getKeepalivehost());
        assertNotNull(fromJson.toString());
    }
}