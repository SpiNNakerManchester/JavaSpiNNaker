/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.machine;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestMultiCastRoutingEntry {

    public TestMultiCastRoutingEntry() {
    }

    @Test
    public void testBasic() {
        var directions = asList(Direction.NORTH, Direction.SOUTH);
        var ids = asList(4, 6, 8);
        int key = 100;
        int mask = 200;
        var instance = new MulticastRoutingEntry(
                key, mask, ids, directions, true);

        assertEquals(key, instance.getKey());
        assertEquals(mask, instance.getMask());
        assertTrue(instance.isDefaultable());

        var decode = new MulticastRoutingEntry(
                key, mask, instance.encode(), true);

        assertThat(decode.getLinkIDs(), contains(directions.toArray()));
        assertThat(decode.getProcessorIDs(), contains(ids.toArray()));
    }


}
