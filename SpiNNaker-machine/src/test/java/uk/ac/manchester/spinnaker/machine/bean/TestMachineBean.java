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
package uk.ac.manchester.spinnaker.machine.bean;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.Machine;

/**
 *
 * @author Christian
 */
public class TestMachineBean {

	@Test
	public void testSpinn4() throws IOException {
		var url = TestMachineBean.class.getResource("/spinn4.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);

		var machine = new Machine(fromJson);
		assertNotNull(machine);
	}

	@Test
	public void testSpinn4Fiddle() throws IOException {
		var url = TestMachineBean.class.getResource("/spinn4_fiddle.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);

		var machine = new Machine(fromJson);
		assertNotNull(machine);
	}

	@Test
	public void testSevenEight() throws IOException {
		var url = TestMachineBean.class.getResource("/test24_12.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);

		var machine = new Machine(fromJson);
		assertEquals(24, machine.machineDimensions.height);
		assertEquals(12, machine.machineDimensions.width);
		assertNotNull(machine);
	}

	public void testPop() throws IOException {
		var url = TestMachineBean.class.getResource("/h40w16.json");
		var mapper = MapperFactory.createMapper();
		var fromJson = mapper.readValue(url, MachineBean.class);

		var machine = new Machine(fromJson);
		assertNotNull(machine);
	}
}
