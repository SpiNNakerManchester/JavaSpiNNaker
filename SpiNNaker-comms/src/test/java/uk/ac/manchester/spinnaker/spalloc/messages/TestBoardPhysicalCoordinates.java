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
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author micro
 */
public class TestBoardPhysicalCoordinates {
    
    @Test
    void testFromJson() throws IOException {
        String json = "[2, 4, 6]";
        ObjectMapper mapper = SpallocClient.createMapper();
        BoardPhysicalCoordinates fromJson = mapper.readValue(json, BoardPhysicalCoordinates.class);
        assertEquals(2, fromJson.getCabinet());
        assertEquals(4, fromJson.getFrame());
        assertEquals(6, fromJson.getBoard());
        
        BoardPhysicalCoordinates direct = new BoardPhysicalCoordinates(2, 4, 6);
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
}
