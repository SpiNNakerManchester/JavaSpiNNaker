/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Christian-B
 */
public class CoreSubsetTest {

    public CoreSubsetTest() {
    }

    /**
     * Test of basic methods, of class CoreSubset.
     */
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
    }

    /**
     * Test of addCore method, of class CoreSubset.
     */
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
        assertNotEquals(subset1.hashCode(), subset3.hashCode());
        assertNotEquals(subset1.hashCode(), subset4.hashCode());
    }

}
