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

import java.util.ArrayList;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
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
        var instance = new CoreSubsets();
        assertEquals(0, instance.size());
        assertTrue(instance.isEmpty());

        instance.addCore(0, 0, 1);
        assertEquals(1, instance.size());
        assertFalse(instance.isEmpty());

        var processors = new ArrayList<Integer>();
        processors.add(1);
        instance.addCores(0, 0, processors);
        assertEquals(1, instance.size());
        assertFalse(instance.isEmpty());
        assertFalse(instance.isChip(ChipLocation.ONE_ZERO));

        processors.add(2);
        instance.addCores(1, 0, processors);
        assertEquals(3, instance.size());
        assertFalse(instance.isEmpty());

        assertTrue(instance.isChip(ChipLocation.ONE_ZERO));
        assertTrue(instance.isCore(new CoreLocation(0, 0, 1)));
        assertFalse(instance.isCore(new CoreLocation(2, 0, 1)));
        assertFalse(instance.isChip(new ChipLocation(3, 1)));
        assertFalse(instance.isCore(new CoreLocation(0, 0, 14)));
    }

    @Test
    public void testAdd() {
        var instance = new CoreSubsets();
        instance.addCore(new CoreLocation(0,0,1));
        //get hashcode to make subset immutable
        @SuppressWarnings("unused")
        int hash = instance.hashCode();
        assertThrows(IllegalStateException.class, () -> {
            instance.addCore(new CoreLocation(0,0,2));
        });
        assertThrows(IllegalStateException.class, () -> {
            instance.addCores(new ChipLocation(0,0), asList(1, 2, 3));
        });
        assertThrows(IllegalStateException.class, () -> {
            instance.addCore(new ChipLocation(0,0), 2);
        });
    }

    public void testMultiple() {
        var locations = new ArrayList<CoreLocation>();
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
        var css = new CoreSubsets(locations);

        var locations2 = new ArrayList<CoreLocation>();
        locations2.add(new CoreLocation(0, 0, 4));
        locations2.add(new CoreLocation(0, 0, 5));
        locations2.add(new CoreLocation(0, 0, 6));
        css.addCores(locations2);

        assertTrue(css.isChip(new ChipLocation(0, 1)));
        assertTrue(css.isCore(new CoreLocation(0, 0, 6)));

        assertTrue(css.isCore(new CoreLocation(0, 1, 3)));

        int count = 0;
        for (var coreLocation: css) {
            count += 1;
            assertEquals(0, coreLocation.getX());
        }
        assertEquals(9, count);

        count = 0;
        for (var coreLocation: css.coreByChip(ChipLocation.ZERO_ZERO)) {
            count += 1;
            assertEquals(0, coreLocation.getX());
            assertEquals(0, coreLocation.getY());
        }
        assertEquals(6, count);
    }

    public void testInterest() {
        var css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), asList(1, 2, 3));
        css1.addCore(new ChipLocation(1, 1), 1);
        css1.addCore(new ChipLocation(2, 2), 1);
        assertEquals(8, css1.size());
        assertFalse(css1.isEmpty());

        var css2 = new CoreSubsets();
        css2.addCores(new ChipLocation(0, 0), asList(2, 3, 5));
        css2.addCores(new ChipLocation(1, 0), asList(1, 2, 3));
        css2.addCores(new ChipLocation(1, 1), asList(9, 7, 1, 5));
        css2.addCore(new ChipLocation(2, 2), 2);
        assertEquals(11, css2.size());
        assertFalse(css2.isEmpty());

        var css3 = css1.intersection(css2);
        assertTrue(css3.isCore(new CoreLocation(0, 0, 2)));
        assertTrue(css3.isCore(new CoreLocation(0, 0, 3)));
        assertTrue(css3.isCore(new CoreLocation(1, 1, 1)));
        assertEquals(3, css3.size());
        assertFalse(css3.isEmpty());
     }

    public void testEquals() {
        var css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), asList(1, 2, 3));

        var css2 = new CoreSubsets();
        css2.addCores(new ChipLocation(0, 0), asList(1, 2, 3));
        css2.addCores(new ChipLocation(0, 1), asList(1, 3));

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
        var css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), asList(1, 2, 3));
        int count = 0;
        for (var coreLocation: css1) {
            count += 1;
            assertThat("p > 0", coreLocation.getP(), greaterThan(0));
            assertThat("p < 4", coreLocation.getP(), lessThan(4));
        }
        assertEquals(6, count);
    }

    public void testByChip() {
        var css1 = new CoreSubsets();
        css1.addCores(new ChipLocation(0, 0), asList(1, 2, 3));
        css1.addCores(new ChipLocation(0, 1), asList(1, 2, 3));
        int count = 0;
        for (var chip: css1.getChips()) {
            for (var core: css1.coreByChip(chip)) {
                count += 1;
                assertEquals(core.getX(), chip.getX());
                assertEquals(core.getX(), chip.getX());
            }
        }
        assertEquals(6, count);
        count = 0;
        for (var chip: css1.getChips()) {
            for (var p: css1.pByChip(chip)) {
                count += 1;
                assertThat("p > 0", p, greaterThan(0));
                assertThat("p < 4", p, lessThan(4));
            }
        }
        assertEquals(6, count);
    }

    public void testBadIterator() {
        var css1 = new CoreSubsets();
        int count = 0;
        for (@SuppressWarnings("unused") var coreLocation: css1) {
            count += 1;
        }
        assertEquals(0, count);

        var empty = css1.coreByChip(ChipLocation.ZERO_ZERO);
        assertEquals(0, empty.size());

        var emptyP = css1.pByChip(ChipLocation.ZERO_ZERO);
        assertEquals(0, emptyP.size());

        assertThrows(NoSuchElementException.class, () -> {
            css1.iterator().next();
        });
    }

}
