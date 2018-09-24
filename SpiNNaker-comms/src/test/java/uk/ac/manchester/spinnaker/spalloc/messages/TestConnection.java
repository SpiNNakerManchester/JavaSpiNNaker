/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author micro
 */
public class TestConnection {
    
    @Test
    void testFromJson() throws IOException {
        String json = "[[2,4],\"6.8.10.12\"]]";
        ObjectMapper mapper = SpallocClient.createMapper();
        Connection fromJson = mapper.readValue(json, Connection.class);
        assertEquals(new ChipLocation(2,4), fromJson.getChip());
        assertEquals("6.8.10.12", fromJson.getHostname());
        
        Connection direct = new Connection(new Chip(2, 4), "6.8.10.12");
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
}
