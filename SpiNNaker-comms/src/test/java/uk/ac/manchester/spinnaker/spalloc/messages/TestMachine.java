package uk.ac.manchester.spinnaker.spalloc.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestMachine {

    @Test
    void testFromJson() throws IOException {
        String json = "{\"name\":\"power-monitor\","
                + "\"tags\":[\"power-monitor\",\"machine-room\"],"
                + "\"width\":1,\"height\":1,"
                + "\"dead_boards\":[[0,0,1],[0,0,2]],\"dead_links\":[]}";
        ObjectMapper mapper = SpallocClient.createMapper();
        Machine fromJson = mapper.readValue(json, Machine.class);
        assertEquals("power-monitor", fromJson.getName());
        assertThat(fromJson.getTags(), contains("power-monitor","machine-room"));
        assertEquals(1, fromJson.getWidth());
        assertEquals(1, fromJson.getHeight());
        assertThat(fromJson.getDeadBoards(), contains(
                new BoardCoordinates(0, 0, 1), new BoardCoordinates(0,0,2)));
        assertEquals(0, fromJson.getDeadLinks().size());
        assertNotNull(fromJson.toString());
    }

    @Test
    void testAssumedDeadLinks() throws IOException {
        String json = "{\"name\":\"power-monitor\","
                + "\"tags\":[\"power-monitor\",\"machine-room\"],"
                + "\"width\":1,\"height\":1,"
                + "\"dead_boards\":[[1,2,3],[4,5,6]],"
                + "\"dead_links\":[[7,8,9,10],[11,12,13,14]]}";
        ObjectMapper mapper = SpallocClient.createMapper();
        Machine fromJson = mapper.readValue(json, Machine.class);
        assertEquals("power-monitor", fromJson.getName());
        assertThat(fromJson.getTags(), contains("power-monitor","machine-room"));
        assertEquals(1, fromJson.getWidth());
        assertEquals(1, fromJson.getHeight());
        assertThat(fromJson.getDeadBoards(), contains(
                new BoardCoordinates(1, 2, 3), new BoardCoordinates(4, 5, 6)));
        assertEquals(2, fromJson.getDeadLinks().size());
        assertEquals(7, fromJson.getDeadLinks().get(0).getX());
        assertEquals(14, fromJson.getDeadLinks().get(1).getLink());
        assertNotNull(fromJson.toString());
    }

	@Test
	void testNullJson() throws IOException {
		String json = "{\"name\":null}";
		ObjectMapper mapper = SpallocClient.createMapper();
		Machine fromJson = mapper.readValue(json, Machine.class);
		assertNull(fromJson.getName());
		assert fromJson.getTags().isEmpty() : "must have no tags";
		assertEquals(0, fromJson.getWidth());
		assertEquals(0, fromJson.getHeight());
		assert fromJson.getDeadBoards().isEmpty() : "must have no dead boards";
		assert fromJson.getDeadLinks().isEmpty() : "must have no dead links";
		assertNotNull(fromJson.toString());
	}
}