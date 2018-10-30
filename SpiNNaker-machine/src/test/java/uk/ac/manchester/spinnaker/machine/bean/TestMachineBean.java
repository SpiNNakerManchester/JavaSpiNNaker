package uk.ac.manchester.spinnaker.machine.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.manchester.spinnaker.machine.Machine;

/**
 *
 * @author Christian
 */
public class TestMachineBean {

    @Test
    public void testSpinn4() throws IOException {
        URL url = TestMachineBean.class.getResource("/spinn4.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        MachineBean fromJson = mapper.readValue(url, MachineBean.class);

        Machine machine = new Machine(fromJson);
        assertNotNull(machine);
    }

    @Test
    public void testSpinn4Fiddle() throws IOException {
        URL url = TestMachineBean.class.getResource("/spinn4_fiddle.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        MachineBean fromJson = mapper.readValue(url, MachineBean.class);

        Machine machine = new Machine(fromJson);
        assertNotNull(machine);
    }

}