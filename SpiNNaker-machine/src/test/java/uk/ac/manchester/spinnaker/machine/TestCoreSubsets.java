/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


/**
 *
 * @author Christian-B
 */
public class TestCoreSubsets {

    public TestCoreSubsets() {
    }

    /**
     * Test of basic methods, of class CoreSubset.
     */
    @Test
    public void testBasic() {
        CoreSubsets instance = new CoreSubsets();
        assertEquals(0, instance.size());

        instance.addCore(0, 0, 1);
        assertEquals(1, instance.size());

        ArrayList<Integer> processors = new ArrayList();
        processors.add(1);
        instance.addCores(0, 0, processors);
        assertEquals(1, instance.size());
        assertFalse(instance.isChip(ChipLocation.ONE_ZERO));

        processors.add(2);
        instance.addCores(1, 0, processors);
        assertEquals(3, instance.size());

        assertTrue(instance.isChip(ChipLocation.ONE_ZERO));
        assertTrue(instance.isCore(new CoreLocation(0, 0, 1)));
        assertFalse(instance.isCore(new CoreLocation(2, 0, 1)));
        assertFalse(instance.isChip(new ChipLocation(3, 1)));
        assertFalse(instance.isCore(new CoreLocation(0, 0, 14)));
    }

    @Test
    public void testAdd() {
        CoreSubsets instance = new CoreSubsets();
        instance.addCore(new CoreLocation(0,0,1));
        //get hashcode to make subset immutable
        int hash = instance.hashCode();
        assertThrows(IllegalStateException.class, () -> {
            instance.addCore(new CoreLocation(0,0,2));
        });
        assertThrows(IllegalStateException.class, () -> {
            instance.addCores(new ChipLocation(0,0), Arrays.asList(1, 2, 3));
        });
        assertThrows(IllegalStateException.class, () -> {
            instance.addCore(new ChipLocation(0,0), 2);
        });
        CoreSubset cs1 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> {
            instance.addSubset(cs1);
        });
    }

    public void testMultiple() {
        CoreSubset cs1 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs2 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(4, 5, 6));
        CoreSubset cs3 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        CoreSubset cs4 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs5 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3, 4));
        CoreSubsets css = new CoreSubsets(
                Arrays.asList(cs1, cs2, cs3, cs4, cs5));

        assertTrue(css.isChip(new ChipLocation(0, 1)));
        assertTrue(css.isCore(new CoreLocation(0, 0, 6)));
        assertEquals(cs3, css.getCoreSubset(new ChipLocation(0, 1)));

        for (CoreSubset subset:css.coreSubsets()) {
             assertTrue(subset.contains(3));
        }

        assertNull(css.getCoreSubset(ChipLocation.ONE_ZERO));

        int count = 0;
        for (CoreLocation coreLocation: css.coreIterable()) {
            count += 1;
            assertEquals(0, coreLocation.getX());
        }
        assertEquals(9, count);

        count = 0;
        for (CoreLocation coreLocation: css.coreLocations(ChipLocation.ZERO_ZERO)) {
            count += 1;
            assertEquals(0, coreLocation.getX());
            assertEquals(0, coreLocation.getY());
        }
        assertEquals(6, count);
    }

    public void testInterest() {
        CoreSubset cs11 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs12 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        CoreSubset cs13 = new CoreSubset(
                new ChipLocation(1, 1), 1);
        CoreSubset cs14 = new CoreSubset(
                new ChipLocation(2, 2), 1);
        CoreSubsets css1 = new CoreSubsets(
                    Arrays.asList(cs11, cs12, cs13, cs14));

        CoreSubset cs21 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(2, 3, 5));
        CoreSubset cs22 = new CoreSubset(
                new ChipLocation(1, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs23 = new CoreSubset(
                new ChipLocation(1, 1), Arrays.asList(9, 7, 1, 5));
        CoreSubset cs24 = new CoreSubset(
                new ChipLocation(2, 2), 2);
        CoreSubsets css2 = new CoreSubsets(
                    Arrays.asList(cs21, cs22, cs23, cs24));
        assertEquals(11, css2.size());

        CoreSubsets css3 = css1.intersection(css2);
        assertTrue(css3.isCore(new CoreLocation(0, 0, 2)));
        assertTrue(css3.isCore(new CoreLocation(0, 0, 3)));
        assertTrue(css3.isCore(new CoreLocation(1, 1, 1)));
        assertEquals(3, css3.size());

     }

    public void testEquals() {
        CoreSubset cs11 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs12 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        CoreSubsets css1 = new CoreSubsets(
                    Arrays.asList(cs11, cs12));

        CoreSubset cs21 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 3, 2));
        CoreSubset cs22 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 3));
        CoreSubsets css2 = new CoreSubsets(
                    Arrays.asList(cs21, cs22));

        assertNotEquals(css1, css2);
        assertNotEquals(css1.toString(), css2.toString());

        css2.addCore(new CoreLocation (0,1,2));
        assertEquals(css1, css2);
        assertEquals(css1.toString(), css2.toString());
        assertEquals(css1, css1);

        assertNotEquals(css1, "css1");
        assertNotEquals(css1, null);
    }

    public void testIterator() {
        CoreSubset cs11 = new CoreSubset(
                new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        CoreSubset cs12 = new CoreSubset(
                new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        CoreSubsets css1 = new CoreSubsets(
                    Arrays.asList(cs11, cs12));
        int count = 0;
        for (CoreLocation coreLocation:css1.coreIterable()) {
            count += 1;
            assertThat("p > 0", coreLocation.getP(), greaterThan(0));
            assertThat("p < 4", coreLocation.getP(), lessThan(4));
        }
        assertEquals(6, count);
    }

    public void testBadIterator() {
        CoreSubsets css1 = new CoreSubsets();
        int count = 0;
        for (CoreLocation coreLocation:css1.coreIterable()) {
            count += 1;
        }
        assertEquals(0, count);

        Collection<CoreLocation>  empty = css1.coreLocations(
                ChipLocation.ZERO_ZERO);
        assertEquals(0, empty.size());

        Iterable<CoreLocation> nodata = css1.coreIterable();
        assertThrows(NoSuchElementException.class, () -> {
            nodata.iterator().next();
        });
    }

}
