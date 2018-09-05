package uk.ac.manchester.spinnaker.machine;

import java.util.HashMap;
import java.util.Map;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class TestCoreLocation {

	@Test
	public void testChipLocationBasicUse() {
		CoreLocation l1 = new CoreLocation(0, 0, 0);
		assertEquals(l1, l1);
		assertNotEquals(l1, "l1");

		CoreLocation l2 = new CoreLocation(0, 0, 0);
		assertEquals(0, l2.getX());
		assertEquals(0, l2.getY());
		assertEquals(0, l2.getP());
		assertEquals(l1, l2);
		assertEquals(l1.toString(), l2.toString());
		assertTrue(l2.onSameCoreAs(l1));

        CoreLocation l3 = new CoreLocation(0, 1, 0);
		assertEquals(0, l3.getX());
		assertEquals(1, l3.getY());
		assertEquals(0, l3.getP());
		assertNotEquals(l1, l3);
		assertNotEquals(l1.toString(), l3.toString());

        CoreLocation l4 = new CoreLocation(ChipLocation.ONE_ZERO, 0);
		assertEquals(1, l4.getX());
		assertEquals(0, l4.getY());
		assertEquals(0, l4.getP());
		assertNotEquals(l1, l4);

        CoreLocation l5 = new CoreLocation(0, 0, 1);
		assertEquals(0, l5.getX());
		assertEquals(0, l5.getY());
		assertEquals(1, l5.getP());
		assertNotEquals(l1, l5);
		assertTrue(l1.onSameChipAs(l5));
		assertFalse(l3.onSameChipAs(l4));

		Map<CoreLocation, Integer> m = new HashMap<>();
		m.put(l1, 123);
		assertEquals(123, (int) m.get(l2));

        assertEquals(l1, l1.asCoreLocation());

	}

	@Test
	public void testChipLocationRangesXmin() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(-1, 0, 0);
           });
	}

	@Test
	public void testChipLocationRangesYmin() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(0, -1, 0);
            });
        }

	@Test
        public void testChipLocationRangesPmin() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(0, 0, -1);
           });
	}

	@Test
        public void testChipLocationRangesXmax() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(257, 0, 0);
           });
	}

	@Test
	public void testChipLocationRangesYmax() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(0, 257, 0);
           });
	}

	@Test
	public void testChipLocationRangesPmax() {
           assertThrows(IllegalArgumentException.class, () -> {
		new CoreLocation(0, 0, 18);
           });
        }

    private class MockCoreLocation implements HasCoreLocation{

        @Override
        public int getX() {
            return 1;
        }

        @Override
        public int getY() {
            return 2;
        }

        @Override
        public int getP() {
            return 3;
        }

    }

    @Test
    public void testAsCoreLocation() {
        assertEquals(new CoreLocation(1, 2, 3),
                new MockCoreLocation().asCoreLocation());
    }

        @Test
    public void testCompare() {
        CoreLocation core001 = new CoreLocation(0, 0, 1);
        CoreLocation core001a = new CoreLocation(0, 0, 1);
        CoreLocation core002 = new CoreLocation(0, 0, 2);
        CoreLocation core102 = new CoreLocation(1, 0, 2);
        CoreLocation core013 = new CoreLocation(0, 1, 3);
        CoreLocation core114 = new CoreLocation(1, 1, 4);
        assertThat("114 > 001", core114, greaterThan(core001));
        assertThat("114 > 013", core114, greaterThan(core013));
        assertThat("114 > 102", core114, greaterThan(core102));
        assertThat("102 > 114", core102, lessThan(core114));
        assertThat("102 < 013", core102, greaterThan(core013));
        assertThat("013 > 102", core013, lessThan(core102));
        assertThat("114 < 102", core114, greaterThan(core102));
        assertThat("001 < 002", core001, lessThan(core002));
        assertThat("002 < 001", core002, greaterThan(core001));
        assertEquals(0, core001.compareTo(core001a));
        assertEquals(0, core001.compareTo(core001));
    }

}
