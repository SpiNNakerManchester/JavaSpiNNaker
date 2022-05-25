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
				+ "\"start_time\":1.537284307847865E9,"
				+ "\"keepalivehost\":\"86.82.216.229\"}";
		var mapper = SpallocClient.createMapper();
		var fromJson = mapper.readValue(json, JobState.class);
		assertEquals(State.POWER, fromJson.getState());
		assertEquals(true, fromJson.getPower());
		assertEquals(1537284307.847865f, fromJson.getStartTime());
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
