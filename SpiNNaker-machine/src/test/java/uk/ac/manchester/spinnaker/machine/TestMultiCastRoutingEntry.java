/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.Arrays;
import java.util.List;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 *
 * @author Christian-B
 */
public class TestMultiCastRoutingEntry {

    public TestMultiCastRoutingEntry() {
    }

    @Test
    public void testBasic() {
        List<Direction> directions = Arrays.asList(Direction.NORTH, Direction.SOUTH);
        List<Integer> ids = Arrays.asList(4, 6, 8);
        int key = 100;
        int mask = 200;
        MulticastRoutingEntry instance = new MulticastRoutingEntry(
                key, mask, ids, directions, true);

        assertEquals(key, instance.getKey());
        assertEquals(mask, instance.getMask());
        assertTrue(instance.isDefaultable());

        MulticastRoutingEntry decode = new MulticastRoutingEntry(
                key, mask, instance.encode(), true);

        assertThat(decode.getLinkIDs(), contains(directions.toArray()));
        assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
    }


}
