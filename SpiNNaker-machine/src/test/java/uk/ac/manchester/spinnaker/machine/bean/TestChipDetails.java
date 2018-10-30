package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian
 */
public class TestChipDetails {

    @Test
    public void testFromJson() throws IOException {
        String json = "{\"cores\": 18, \"deadLinks\": [3, 4, 5], "
                + "\"ipAddress\": \"130.88.192.243\", \"ethernet\":[0, 0]}";
        //String json = "{\"cores\": 18, \"ipAddress\": \"130.88.192.243\", \"ethernet\":[0, 0]}";
        ObjectMapper mapper = MapperFactory.createMapper();
        ChipDetails fromJson = mapper.readValue(json, ChipDetails.class);
        assertNotNull(fromJson);
    }

}