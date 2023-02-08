/*
 * Copyright (c) 2018 The University of Manchester
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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestMachineVersion {

	public TestMachineVersion() {
	}

	@Test
	public void testById() {
		assertEquals(MachineVersion.FIVE, MachineVersion.byId(5));
		assertThrows(IllegalArgumentException.class, () -> {
			MachineVersion.byId(1);
		});
	}

	@Test
	public void testThree() {
		var instance = MachineVersion.THREE;
		assertEquals(new MachineDimensions(2, 2), instance.machineDimensions);
	}

	@Test
	public void testMachineVersion() {
		assertEquals(MachineVersion.THREE,
				MachineVersion.bySize(new MachineDimensions(2, 2)));
		assertEquals(MachineVersion.FIVE, MachineVersion.bySize(8, 8));
		assertEquals(MachineVersion.THREE_BOARD, MachineVersion.bySize(12, 12));
		assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND,
				MachineVersion.bySize(new MachineDimensions(16, 16)));
		assertEquals(MachineVersion.NONE_TRIAD_LARGE,
				MachineVersion.bySize(new MachineDimensions(20, 20)));
		assertEquals(MachineVersion.TRIAD_WITH_WRAPAROUND,
				MachineVersion.bySize(12 + 24, 12 + 36));
		assertEquals(MachineVersion.TRIAD_NO_WRAPAROUND,
				MachineVersion.bySize(new MachineDimensions(16 + 24, 16 + 36)));
		assertEquals(MachineVersion.NONE_TRIAD_LARGE,
				MachineVersion.bySize(new MachineDimensions(20 + 24, 20 + 36)));
		assertEquals(MachineVersion.TRIAD_WITH_VERTICAL_WRAP,
				MachineVersion.bySize(new MachineDimensions(16, 12)));
		assertEquals(MachineVersion.TRIAD_WITH_HORIZONTAL_WRAP,
				MachineVersion.bySize(new MachineDimensions(12, 16)));
		assertEquals(MachineVersion.NONE_TRIAD_LARGE,
				MachineVersion.bySize(new MachineDimensions(12, 20)));
		assertEquals(MachineVersion.TWENTYFOUR_BOARD,
				MachineVersion.bySize(new MachineDimensions(48, 24)));
		assertEquals(MachineVersion.TRIAD_WITH_VERTICAL_WRAP,
				MachineVersion.bySize(new MachineDimensions(16, 24)));
		assertEquals(MachineVersion.TRIAD_WITH_HORIZONTAL_WRAP,
				MachineVersion.bySize(new MachineDimensions(48, 16)));
		assertEquals(MachineVersion.EXTENDED_SMALL,
				MachineVersion.bySize(new MachineDimensions(3, 2)));
		assertThrows(IllegalArgumentException.class, () -> {
			@SuppressWarnings("unused")
			var v = MachineVersion.bySize(new MachineDimensions(13, 16));
		});
		assertThrows(IllegalArgumentException.class, () -> {
			MachineVersion.bySize(new MachineDimensions(12, 4));
		});
		assertThrows(IllegalArgumentException.class, () -> {
			MachineVersion.bySize(8, 12);
		});
	}

}
