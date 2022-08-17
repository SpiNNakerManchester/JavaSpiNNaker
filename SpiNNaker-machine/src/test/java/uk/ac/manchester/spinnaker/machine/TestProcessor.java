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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import static org.hamcrest.Matchers.*;

/**
 *
 * @author Christian-B
 */
public class TestProcessor {

	private void checkDifferent(Processor p1, Processor p2) {
		assertNotEquals(p1, p2);
		assertNotEquals(p1.hashCode(), p2.hashCode());
		assertNotEquals(p1.toString(), p2.toString());
	}

	private void checkLess(Processor smaller, Processor larger) {
		checkDifferent(smaller, larger);
		assertThat(smaller, lessThan(larger));
		assertThat(larger, greaterThan(smaller));
	}

	private void checkSame(Processor p1, Processor p2) {
		assertEquals(p1, p2);
		assertEquals(p1.hashCode(), p2.hashCode());
		assertEquals(p1.toString(), p2.toString());
	}

	private void checkDifferent2(Processor p1, Processor p2) {
		assertNotEquals(p1, p2);
		assertNotEquals(p1.hashCode(), p2.hashCode());
	}

	private void checkLess2(Processor smaller, Processor larger) {
		checkDifferent2(smaller, larger);
		assertThat(smaller, lessThan(larger));
		assertThat(larger, greaterThan(smaller));
	}

	@Test
	public void testEquals() {
		var p1 = Processor.factory(1);
		var p2 = Processor.factory(2);
		checkDifferent(p1, p2);
		var p1f = Processor.factory(1, false);
		checkSame(p1, p1f);
		var p1m = Processor.factory(1, true);
		checkDifferent(p1, p1m);
		assertNotEquals(p1, null);
		assertNotEquals(p1, "p1");
		var faster = Processor.factory(1,
				MachineDefaults.PROCESSOR_CLOCK_SPEED * +10000000,
				MachineDefaults.DTCM_AVAILABLE, false);
		var faster2 = Processor.factory(1,
				MachineDefaults.PROCESSOR_CLOCK_SPEED * +10000000,
				MachineDefaults.DTCM_AVAILABLE, false);
		checkSame(faster, faster2);
	}

	@Test
	public void testComparesTo() {
		var p1 = Processor.factory(1);
		var standard =
				Processor.factory(1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
						MachineDefaults.DTCM_AVAILABLE, false);
		var two = Processor.factory(2, MachineDefaults.PROCESSOR_CLOCK_SPEED,
				MachineDefaults.DTCM_AVAILABLE, false);
		var monitor =
				Processor.factory(1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
						MachineDefaults.DTCM_AVAILABLE, true);
		var faster = Processor.factory(1,
				MachineDefaults.PROCESSOR_CLOCK_SPEED * +10000000,
				MachineDefaults.DTCM_AVAILABLE, false);
		var more = Processor.factory(1, MachineDefaults.PROCESSOR_CLOCK_SPEED,
				MachineDefaults.DTCM_AVAILABLE + 10, false);
		assertThat(p1, lessThanOrEqualTo(standard));
		checkLess(standard, two);
		checkLess(standard, monitor);
		checkLess(standard, faster);
		checkLess2(standard, more);
	}

	@Test
	public void testClone() {
		var p1 = Processor.factory(1);
		var p1m = Processor.factory(1, true);
		var clone = p1.cloneAsSystemProcessor();
		checkSame(p1m, clone);
		var faster =
				Processor.factory(1, MachineDefaults.PROCESSOR_CLOCK_SPEED + 10,
						MachineDefaults.DTCM_AVAILABLE, false);
		var fasterM =
				Processor.factory(1, MachineDefaults.PROCESSOR_CLOCK_SPEED + 10,
						MachineDefaults.DTCM_AVAILABLE, true);
		clone = faster.cloneAsSystemProcessor();
		checkSame(fasterM, clone);
	}

	@Test
	public void testCpuCyclesAvailable() {
		var p1 = Processor.factory(1);
		assertEquals(200000, p1.cpuCyclesAvailable());
	}

	@Test
	@SuppressWarnings("unused")
	public void testBad() {
		assertThrows(IllegalArgumentException.class, () -> {
			var bad = Processor.factory(1, -10,
					MachineDefaults.DTCM_AVAILABLE, false);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			var bad = Processor.factory(1,
					MachineDefaults.PROCESSOR_CLOCK_SPEED, -10, false);
		});
	}

}
