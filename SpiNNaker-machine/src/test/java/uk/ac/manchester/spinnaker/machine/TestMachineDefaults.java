/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestMachineDefaults {

    public TestMachineDefaults() {
    }

    //_4_chip_down_links = {
    //    (0, 0, 3), (0, 0, 4), (0, 1, 3), (0, 1, 4),
    //    (1, 0, 0), (1, 0, 1), (1, 1, 0), (1, 1, 1)
    //}

    @Test
    public void testFoutChipDownLinks() {
        //Misuses of CoreLocation!
        ArrayList<CoreLocation> fromPython = new ArrayList();
        fromPython.add(new CoreLocation(0, 0, 3));
        fromPython.add(new CoreLocation(0, 0, 4));
        fromPython.add(new CoreLocation(0, 1, 3));
        fromPython.add(new CoreLocation(0, 1, 4));
        fromPython.add(new CoreLocation(1, 0, 0));
        fromPython.add(new CoreLocation(1, 0, 1));
        fromPython.add(new CoreLocation(1, 1, 0));
        fromPython.add(new CoreLocation(1, 1, 1));

        ArrayList<CoreLocation> fromDefaults = new ArrayList();
        Map<ChipLocation, Set<Direction>> map = MachineDefaults.FOUR_CHIP_DOWN_LINKS;
        for (Entry<ChipLocation, Set<Direction>> entry: map.entrySet()) {
            assertNotNull(entry.getKey());
            for (Direction direction:entry.getValue()) {
                fromDefaults.add(new CoreLocation(entry.getKey(), direction.id));
            }
        }
        assertThat (fromDefaults, containsInAnyOrder(fromPython.toArray()));
    }
}
