/*
 * Copyright (c) 2018-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.machine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.DTCM_AVAILABLE;
import static uk.ac.manchester.spinnaker.machine.MachineDefaults.PROCESSOR_CLOCK_SPEED;
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MEGAHERTZ_PER_HERTZ;

import org.junit.jupiter.api.Test;

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

	private static final int FAST_PROCESSOR =
			PROCESSOR_CLOCK_SPEED + 10 * MEGAHERTZ_PER_HERTZ;

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
		var faster =
				Processor.factory(1, FAST_PROCESSOR, DTCM_AVAILABLE, false);
		var faster2 =
				Processor.factory(1, FAST_PROCESSOR, DTCM_AVAILABLE, false);
		checkSame(faster, faster2);
	}

	@Test
	public void testComparesTo() {
		var p1 = Processor.factory(1);
		var standard = Processor.factory(1, PROCESSOR_CLOCK_SPEED,
				MachineDefaults.DTCM_AVAILABLE, false);
		var two = Processor.factory(2, PROCESSOR_CLOCK_SPEED, DTCM_AVAILABLE,
				false);
		var monitor = Processor.factory(1, PROCESSOR_CLOCK_SPEED,
				DTCM_AVAILABLE, true);
		var faster = Processor.factory(1, FAST_PROCESSOR,
				MachineDefaults.DTCM_AVAILABLE, false);
		var more = Processor.factory(1, PROCESSOR_CLOCK_SPEED,
				DTCM_AVAILABLE + 10, false);
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
		var faster = Processor.factory(1, PROCESSOR_CLOCK_SPEED + 10,
				DTCM_AVAILABLE, false);
		var fasterM = Processor.factory(1, PROCESSOR_CLOCK_SPEED + 10,
				DTCM_AVAILABLE, true);
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
			var bad = Processor.factory(1, -10, DTCM_AVAILABLE, false);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			var bad = Processor.factory(1, PROCESSOR_CLOCK_SPEED, -10, false);
		});
	}

}
