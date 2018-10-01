package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestChipLocationBean {
    
    @Test
    void testFromJson() throws IOException {
        String json = "[2, 4]";
        ObjectMapper mapper = SpallocClient.createMapper();
        ChipLocationBean fromJson = mapper.readValue(json, ChipLocationBean.class);
        assertEquals(2, fromJson.getX());
        assertEquals(4, fromJson.getY());
        
        ChipLocationBean direct = new ChipLocationBean(2, 4);
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
    @Test
    void testNullJson() throws IOException {
        String json = "null";
        ObjectMapper mapper = SpallocClient.createMapper();
        ChipLocationBean fromJson = mapper.readValue(json, ChipLocationBean.class);
        assertNull(fromJson);
    }
}