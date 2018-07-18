/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class TestCoreSubset {

    public TestCoreSubset() {
    }

    @Test
    public void testBasic() {
        ArrayList<Integer> processors = new ArrayList();
        processors.add(1);
        processors.add(2);
        processors.add(3);
        CoreSubset subset = new CoreSubset(ChipLocation.ZERO_ZERO, processors);
        assertEquals(3, subset.size());
        assertEquals(ChipLocation.ZERO_ZERO, subset.chip);
        assertTrue(subset.contains(2));
        assertThat(subset.coreLocations(), contains(
                new CoreLocation (0, 0, 1),
                new CoreLocation (0, 0, 2),
                new CoreLocation (0, 0, 3)));
        int count = 0;
        for (CoreLocation coreLocation: subset.coreIterable()) {
            count += 1;
            assertEquals(coreLocation.asChipLocation(), ChipLocation.ZERO_ZERO);
        }
        assertEquals(3, count);
    }

    @Test
    public void testEquals() {
        ArrayList<Integer> processors1 = new ArrayList();
        processors1.add(1);
        processors1.add(2);
        processors1.add(3);
        CoreSubset subset1 = new CoreSubset(ChipLocation.ZERO_ZERO, processors1);
        HashSet<Integer> processors2 = new HashSet();
        processors2.add(1);
        processors2.add(2);
        processors2.add(3);
        CoreSubset subset2 = new CoreSubset(new ChipLocation(0, 0), processors2);
        ArrayList<Integer> processors3 = new ArrayList();
        processors3.add(1);
        processors3.add(2);
        CoreSubset subset3 = new CoreSubset(ChipLocation.ZERO_ZERO, processors3);
        CoreSubset subset4 = new CoreSubset(ChipLocation.ONE_ZERO, processors1);
        assertEquals(subset1, subset2);
        assertNotEquals(subset1, subset3);
        assertNotEquals(subset1, subset4);
        assertEquals(subset1.hashCode(), subset2.hashCode());
        assertEquals(subset1.toString(), subset2.toString());
        assertNotEquals(subset1.hashCode(), subset3.hashCode());
        assertNotEquals(subset1.toString(), subset3.toString());
        assertNotEquals(subset1.hashCode(), subset4.hashCode());
        assertNotEquals(subset1, null);
        assertNotEquals(subset2, "cs1");
     }

    @Test
    public void testImutable() {
        CoreSubset cs1 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        //get hashcode to make subset immutable
        int hash = cs1.hashCode();
        assertThrows(IllegalStateException.class, () -> {
            cs1.addCore(new CoreLocation(0, 0, 5));
        });
        assertThrows(IllegalStateException.class, () -> {
            cs1.addCore(5);
        });
        assertThrows(IllegalStateException.class, () -> {
            cs1.addCore((Integer)5);
        });
        assertThrows(IllegalStateException.class, () -> {
            cs1.addCores(Arrays.asList(5, 6, 7));
        });
        assertEquals(3, cs1.size());
    }

    @Test
    public void testAddCore() {
        CoreSubset cs1 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));

        cs1.addCore(new CoreLocation (0, 0, 4));
        assertEquals(4, cs1.size());
        assertThrows(IllegalArgumentException.class, () -> {
            cs1.addCore(new CoreLocation(1, 0, 5));
        });
    }

    public void testInterect() {
        CoreSubset cs11 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs12 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 2, 3));

        CoreSubset cs21 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(2, 3, 5));

        CoreSubset cs3 = cs11.intersection(cs21);
        assertEquals(2, cs3.size());
        assertThrows(IllegalArgumentException.class, () -> {
            cs12.intersection(cs21);
        });

     }
}
