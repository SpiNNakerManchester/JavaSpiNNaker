package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christain
 */
public class TestConnection {
    
    @Test
    void testFromJson() throws IOException {
        String json = "[[2,4],\"6.8.10.12\"]";
        ObjectMapper mapper = SpallocClient.createMapper();
        Connection fromJson = mapper.readValue(json, Connection.class);
        assertEquals(new ChipLocation(2,4), fromJson.getChip());
        assertEquals("6.8.10.12", fromJson.getHostname());
        
        Connection direct = new Connection(new ChipLocationBean(2, 4), "6.8.10.12");
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
    @Test
    void testNulls() throws IOException {
        String json = "[null,null]";
        ObjectMapper mapper = SpallocClient.createMapper();
        Connection fromJson = mapper.readValue(json, Connection.class);
        assertNull(fromJson.getChip());
        assertNull(fromJson.getHostname());
        
        Connection direct = new Connection(null, null);
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
}
