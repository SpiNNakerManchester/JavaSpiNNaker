package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian
 */
public class TestMachineBean {

    @Test
    public void testFromJson() throws IOException {
        URL url = TestMachineBean.class.getResource("/spinn-4.json");
        System.out.println(url);
        //String json = "[2, 4]";
        ObjectMapper mapper = MapperFactory.createMapper();
        MachineBean fromJson = mapper.readValue(url, MachineBean.class);
        System.out.println(fromJson);
        //assertEquals(2, fromJson.getX());
        //assertEquals(4, fromJson.getY());

    }


}