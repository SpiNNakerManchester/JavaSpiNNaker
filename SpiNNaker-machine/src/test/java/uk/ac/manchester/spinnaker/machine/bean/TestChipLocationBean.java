package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 *
 * @author Christian
 */
public class TestChipLocationBean {

    @Test
    public void testFromJson() throws IOException {
        String json = "[2, 4]";
        ObjectMapper mapper = MapperFactory.createMapper();
        ChipLocation fromJson = mapper.readValue(json, ChipLocation.class);
        assertEquals(2, fromJson.getX());
        assertEquals(4, fromJson.getY());

        ChipLocation direct = new ChipLocation(2, 4);
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }

    @Test
    public void testNullJson() throws IOException {
        String json = "null";
        ObjectMapper mapper = MapperFactory.createMapper();
        ChipLocation fromJson = mapper.readValue(json, ChipLocation.class);
        assertNull(fromJson);
    }

    @Test
    public void testOneNullJson() throws IOException {
        String json = "[2]";
        ObjectMapper mapper = MapperFactory.createMapper();
        assertThrows(MismatchedInputException.class, () -> {
            mapper.readValue(json, ChipLocation.class);
        });
    }

}