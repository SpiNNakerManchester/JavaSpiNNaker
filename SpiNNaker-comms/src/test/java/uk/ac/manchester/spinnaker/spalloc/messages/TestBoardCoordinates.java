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
public class TestBoardCoordinates {
    
    @Test
    void testFromJson() throws IOException {
        String json = "[2, 4, 6]";
        ObjectMapper mapper = SpallocClient.createMapper();
        BoardCoordinates fromJson = mapper.readValue(json, BoardCoordinates.class);
        assertEquals(2, fromJson.getX());
        assertEquals(4, fromJson.getY());
        assertEquals(6, fromJson.getZ());
        
        BoardCoordinates direct = new BoardCoordinates(2, 4, 6);
        assertEquals(direct, fromJson);
        assertEquals(direct.hashCode(), fromJson.hashCode());
        assertEquals(direct.toString(), fromJson.toString());
    }
    
}
