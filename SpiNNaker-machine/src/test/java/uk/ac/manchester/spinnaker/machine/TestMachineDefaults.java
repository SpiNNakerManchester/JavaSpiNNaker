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

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Christian-B
 */
public class TestMachineDefaults {

	// _4_chip_down_links = {
	//     (0, 0, 3), (0, 0, 4), (0, 1, 3), (0, 1, 4),
	//     (1, 0, 0), (1, 0, 1), (1, 1, 0), (1, 1, 1)
	// }

	@Test
	public void testFourChipDownLinks() {
		// Misuses of CoreLocation!
		var fromPython = List.of(//
				new CoreLocation(0, 0, 3), new CoreLocation(0, 0, 4),
				new CoreLocation(0, 1, 3), new CoreLocation(0, 1, 4),
				new CoreLocation(1, 0, 0), new CoreLocation(1, 0, 1),
				new CoreLocation(1, 1, 0), new CoreLocation(1, 1, 1));

		var fromDefaults = new ArrayList<CoreLocation>();
		MachineDefaults.FOUR_CHIP_DOWN_LINKS.forEach((chip, dirs) -> {
			assertNotNull(chip);
			dirs.stream().map(direction -> new CoreLocation(chip, direction.id))
					.forEach(fromDefaults::add);
		});
		assertThat(fromDefaults, containsInAnyOrder(fromPython.toArray()));
	}
}
