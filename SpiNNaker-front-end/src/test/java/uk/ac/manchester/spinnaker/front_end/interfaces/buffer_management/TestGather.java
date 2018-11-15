/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.bean.MapperFactory;

/**
 *
 * @author Christian-B
 */
public class TestGather {

    @Test
    public void testSimpleJson() throws IOException {
        URL url = TestGather.class.getResource("/gather.json");
        ObjectMapper mapper = MapperFactory.createMapper();
        List<Gather> fromJson = mapper.readValue(
                url, new TypeReference<List<Gather>>() { });
        assertEquals(1, fromJson.size());
    }

}
