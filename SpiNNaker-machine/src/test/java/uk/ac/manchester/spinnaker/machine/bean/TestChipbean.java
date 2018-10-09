package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Processor;

/**
 *
 * @author Christian
 */
public class TestChipbean {

    @Test
    public void testFromJson() throws IOException {
        String json = "[1, 2, {\"cores\": 17, \"ethernet\": [2, 3]}, {\"routerClockSpeed\": 1013, \"sdram\": 123469692, \"routerEntries\": 1013, \"monitors\": 2, \"virtual\": true}]";
        ObjectMapper mapper = MapperFactory.createMapper();
        ChipBean fromJson = mapper.readValue(json, ChipBean.class);
        System.out.println(fromJson);
    }

}