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
package uk.ac.manchester.spinnaker.front_end.download;

import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.manchester.spinnaker.machine.bean.MapperFactory.createMapper;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.front_end.download.request.Gather;

/**
 *
 * @author Christian-B
 */
public class TestGather {

	@Test
	public void testSimpleJson() throws IOException {
		var url = TestGather.class.getResource("/gather.json");
		var fromJson = createMapper().readValue(url, Gather.LIST);
		assertEquals(1, fromJson.size());
	}

}
