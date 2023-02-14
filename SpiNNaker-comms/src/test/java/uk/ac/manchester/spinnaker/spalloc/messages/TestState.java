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
package uk.ac.manchester.spinnaker.spalloc.messages;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.spalloc.SpallocClient;

/**
 *
 * @author Christian
 */
public class TestState {

	@Test
	void testFromJson() throws IOException {
		var json = "{\"state\":2,"
				+ "\"power\":true,"
				+ "\"keepalive\":60.0,"
				+ "\"reason\":null,"
				+ "\"start_time\":1.125,"
				+ "\"keepalivehost\":\"86.82.216.229\"}";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobState.class);
		assertEquals(State.POWER, fromJson.getState());
		assertEquals(true, fromJson.getPower());
		assertEquals(1.125, fromJson.getStartTime());
		assertEquals(60, fromJson.getKeepalive());
		assertNull(fromJson.getReason());
		assertEquals("86.82.216.229", fromJson.getKeepalivehost());
		assertNotNull(fromJson.toString());
	}

	@Test
	void testNullJson() throws IOException {
		var json = "{\"reason\":null}";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobState.class);
		assertNull(fromJson.getState());
		assertNull(fromJson.getPower());
		assertNull(fromJson.getReason());
		assertEquals(0.0, fromJson.getStartTime());
		assertNull(fromJson.getKeepalivehost());
		assertNotNull(fromJson.toString());
	}
}
