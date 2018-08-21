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
    }

    public void testMultiple() {
        ArrayList<CoreLocation> locations = new ArrayList();
        locations.add(new CoreLocation(0, 0, 1));
        locations.add(new CoreLocation(0, 0, 2));
        locations.add(new CoreLocation(0, 0, 3));
        locations.add(new CoreLocation(0, 1, 1));
        locations.add(new CoreLocation(0, 1, 2));
        locations.add(new CoreLocation(0, 1, 3));
        locations.add(new CoreLocation(0, 0, 1));
        locations.add(new CoreLocation(0, 0, 2));
        locations.add(new CoreLocation(0, 0, 3));
        locations.add(new CoreLocation(0, 0, 1));
        locations.add(new CoreLocation(0, 0, 2));
        locations.add(new CoreLocation(0, 0, 3));
        locations.add(new CoreLocation(0, 0, 4));
        CoreSubsets css = new CoreSubsets(locations);

        ArrayList<CoreLocation> locations2 = new ArrayList();
        locations2.add(new CoreLocation(0, 0, 4));
        locations2.add(new CoreLocation(0, 0, 5));
        locations2.add(new CoreLocation(0, 0, 6));
        css.addCores(locations2);

        assertTrue(css.isChip(new ChipLocation(0, 1)));
        assertTrue(css.isCore(new CoreLocation(0, 0, 6)));

        assertTrue(css.isCore(new CoreLocation(0, 1, 3)));

        int count = 0;
        for (CoreLocation coreLocation: css) {
            count += 1;
            assertEquals(0, coreLocation.getX());
        }
        assertEquals(9, count);

        count = 0;
        for (CoreLocation coreLocation: css.coreByChip(ChipLocation.ZERO_ZERO)) {
            count += 1;
            assertEquals(0, coreLocation.getX());
            assertEquals(0, coreLocation.getY());
        }
        assertEquals(6, count);
    }

    public void testInterest() {
        CoreSubsets css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        css1.addCore(new ChipLocation(1, 1), 1);
        css1.addCore(new ChipLocation(2, 2), 1);
        assertEquals(8, css1.size());

        CoreSubsets css2 = new CoreSubsets();
        css2.addCores(new ChipLocation(0, 0), Arrays.asList(2, 3, 5));
        css2.addCores(new ChipLocation(1, 0), Arrays.asList(1, 2, 3));
        css2.addCores(new ChipLocation(1, 1), Arrays.asList(9, 7, 1, 5));
        css2.addCore(new ChipLocation(2, 2), 2);
        assertEquals(11, css2.size());

        CoreSubsets css3 = css1.intersection(css2);
        assertTrue(css3.isCore(new CoreLocation(0, 0, 2)));
        assertTrue(css3.isCore(new CoreLocation(0, 0, 3)));
        assertTrue(css3.isCore(new CoreLocation(1, 1, 1)));
        assertEquals(3, css3.size());

     }

    public void testEquals() {
        CoreSubsets css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), Arrays.asList(1, 2, 3));

        CoreSubsets css2 = new CoreSubsets();
        css2.addCores(new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        css2.addCores(new ChipLocation(0, 1), Arrays.asList(1, 3));

        assertNotEquals(css1, css2);
        assertNotEquals(css1.toString(), css2.toString());

        css2.addCore(new CoreLocation (0, 1, 2));
        assertEquals(css1, css2);
        assertEquals(css1.toString(), css2.toString());
        assertEquals(css1, css1);

        assertNotEquals(css1, "css1");
        assertNotEquals(css1, null);
    }

    public void testIterator() {
        CoreSubsets css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        int count = 0;
        for (CoreLocation coreLocation:css1) {
            count += 1;
            assertThat("p > 0", coreLocation.getP(), greaterThan(0));
            assertThat("p < 4", coreLocation.getP(), lessThan(4));
        }
        assertEquals(6, count);
    }

    public void testByChip() {
        CoreSubsets css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), Arrays.asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), Arrays.asList(1, 2, 3));
        int count = 0;
        for (ChipLocation chip:css1.getChips()) {
            for (CoreLocation core:css1.coreByChip(chip)) {
                count += 1;
                assertEquals(core.getX(), chip.getX());
                assertEquals(core.getX(), chip.getX());
            }
        }
        assertEquals(6, count);
        count = 0;
        for (ChipLocation chip:css1.getChips()) {
            for (Integer p:css1.pByChip(chip)) {
                count += 1;
                assertThat("p > 0", p, greaterThan(0));
                assertThat("p < 4", p, lessThan(4));
            }
        }
        assertEquals(6, count);
    }

    public void testBadIterator() {
        CoreSubsets css1 = new CoreSubsets();
        int count = 0;
        for (CoreLocation coreLocation:css1) {
            count += 1;
        }
        assertEquals(0, count);

        Collection<CoreLocation>  empty = css1.coreByChip(ChipLocation.ZERO_ZERO);
        assertEquals(0, empty.size());

        Collection<Integer>  emptyP = css1.pByChip(ChipLocation.ZERO_ZERO);
        assertEquals(0, emptyP.size());

        assertThrows(NoSuchElementException.class, () -> {
            css1.iterator().next();
        });
    }

}
