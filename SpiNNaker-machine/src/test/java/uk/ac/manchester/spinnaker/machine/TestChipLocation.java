package uk.ac.manchester.spinnaker.machine;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;


public class TestChipLocation {

    @Test
    public void testChipLocationBasicUse() {
        ChipLocation l1 = new ChipLocation(0, 0);
        ChipLocation l2 = new ChipLocation(0, 0);
        assertEquals(0, l2.getX());
        assertEquals(0, l2.getY());
        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());
        ChipLocation l3 = new ChipLocation(0, 1);
        assertEquals(0, l3.getX());
        assertEquals(1, l3.getY());
        assertNotEquals(l1,l3);
        assertNotEquals(l1.hashCode(),l3.hashCode());
        assertNotEquals(l1, "hello");
        ChipLocation l4 = new ChipLocation(1, 0);
        assertEquals(1, l4.getX());
        assertEquals(0, l4.getY());
        assertNotEquals(l1,l4);
        assertNotEquals(l1.hashCode(),l4.hashCode());

        Map<ChipLocation, Integer> m = new HashMap<>();
        m.put(l1, 123);
        assertEquals(123, (int) m.get(l2));
    }

    @Test
    public void testEquals() {
        ChipLocation chip00 = new ChipLocation(0,0);
        ChipLocation chip10 = new ChipLocation(1,0);
        ChipLocation chip01 = new ChipLocation(0,1);
        ChipLocation chip11 = new ChipLocation(1,1);
        assertThat("11 > 00", chip11, greaterThan(chip00));
        assertThat("11 > 01", chip11, greaterThan(chip01));
        assertThat("11 > 10", chip11, greaterThan(chip10));
        assertThat("10 > 11", chip10, lessThan(chip11));
        assertThat("10 < 01", chip10, greaterThan(chip01));
        assertThat("01 > 10", chip01, lessThan(chip10));
        assertThat("10 < 01", chip11, greaterThan(chip10));
    }

    @Test
	public void testChipLocationRangesXmin() {
            assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(-1, 0);
            });
	}

	@Test
	public void testChipLocationRangesYmin() {
           assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(0, -1);
           });
	}

	@Test
	public void testChipLocationRangesXmax() {
           assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(257, 0);
           });
	}

	@Test
	public void testChipLocationRangesYmax() {
          assertThrows(IllegalArgumentException.class, () -> {
		new ChipLocation(0, 257);
          });
	}

    private class MockChipLocation implements HasChipLocation{

        @Override
        public int getX() {
            return 1;
        }

        @Override
        public int getY() {
            return 2;
        }
    }

    @Test
    public void testDefaults() {
        assertEquals(new CoreLocation(1, 2, 0),
                new MockChipLocation().getScampCore());
        assertEquals(new ChipLocation(1, 2),
                new MockChipLocation().asChipLocation());
    }
}
