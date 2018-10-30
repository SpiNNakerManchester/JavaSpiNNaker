package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian
 */
public class TestChipbean {

    @Test
    public void testFromJson() throws IOException {
        String json = "[1, 2, {\"cores\": 17, \"ethernet\": [2, 3]}, {"
                + "\"routerClockSpeed\": 1013, \"sdram\": 123469692, "
                + "\"routerEntries\": 1013, \"monitors\": 2, "
                + "\"virtual\": true}]";
        ObjectMapper mapper = MapperFactory.createMapper();
        ChipBean fromJson = mapper.readValue(json, ChipBean.class);
        assertNotNull(fromJson);
        System.out.println(fromJson);
    }

}