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
	public void testFoutChipDownLinks() {
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
