package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestJobMachineInfo {
    
    @Test
    void testFromJson() throws IOException {
        String json =  "{\"connections\":[[[0,0],\"10.2.225.177\"],"
            + "[[4,8],\"10.2.225.145\"],[[0,12],\"10.2.225.185\"],"
            + "[[8,16],\"10.2.225.121\"],[[4,20],\"10.2.225.153\"],"
            + "[[8,4],\"10.2.225.113\"]],"
            + "\"width\":16,\"machine_name\":\"Spin24b-001\","
            + "\"boards\":[[2,1,1],[2,1,0],[2,1,2],[2,0,2],[2,0,1],[2,0,0]],"
            + "\"height\":24}";
        ObjectMapper mapper = SpallocClient.createMapper();
        JobMachineInfo fromJson = mapper.readValue(json, JobMachineInfo.class);
        assertEquals(6, fromJson.getConnections().size());
        assertEquals(6, fromJson.getBoards().size());
        assertEquals(16, fromJson.getWidth());
        assertEquals(24, fromJson.getHeight());
        assertEquals("Spin24b-001", fromJson.getMachineName());
    }
    
    @Test
    void testNullJson() throws IOException {
        String json = "{\"connections\":null,"
            + "\"machine_name\":null,"
            + "\"boards\":null}";
        ObjectMapper mapper = SpallocClient.createMapper();
        JobMachineInfo fromJson = mapper.readValue(json, JobMachineInfo.class);
        assertEquals(0, fromJson.getConnections().size());
        assertEquals(0, fromJson.getBoards().size());
        assertEquals(0, fromJson.getWidth());
        assertEquals(0, fromJson.getHeight());
        assertNull(fromJson.getMachineName());
        assertNotNull(fromJson.toString());
    }
}