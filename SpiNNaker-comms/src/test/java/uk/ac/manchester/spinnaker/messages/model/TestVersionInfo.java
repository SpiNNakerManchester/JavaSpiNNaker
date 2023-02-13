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
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteBuffer.allocate;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestVersionInfo {
	private ByteBuffer packVersionData(int arg1, int arg2, int arg3,
			byte[] data) {
		var buffer = allocate(25).order(LITTLE_ENDIAN);
		buffer.putInt(arg1).putInt(arg2).putInt(arg3).put(data).flip();
		return buffer;
	}

	@Test
	void testRetrievingBitsFromVersionData()
			throws UnsupportedEncodingException {
		int p2pAddr = 0xf0a1;
		int physCPU = 0xff;
		int virtCPU = 0x0b;
		int verNumber = 0xff;
		int arg1 = (p2pAddr << 16) | (physCPU << 8) | virtCPU;
		int bufferSize = 0x10;
		int arg2 = (verNumber << 16) | bufferSize;
		int buildDate = 0x1000;
		int arg3 = buildDate;
		var data = "my/spinnaker".getBytes("ASCII");

		var versionData = packVersionData(arg1, arg2, arg3, data);

		var vi = new VersionInfo(versionData, false);
		assertEquals("my", vi.name);
		assertEquals(new Version(2, 55, 0), vi.versionNumber);
		assertEquals("spinnaker", vi.hardware);
		assertEquals(new CoreLocation(0xf0, 0xa1, 0x0b), vi.core);
		assertEquals(buildDate, vi.buildDate);
		assertEquals("my/spinnaker", vi.versionString);
	}

	@Test
	void testInvalidVersionDataFormat() throws UnsupportedEncodingException {
		int p2pAddr = 0xf0a1;
		int physCPU = 0xff;
		int virtCPU = 0x0b;
		int verNumber = 0xff;
		int arg1 = (p2pAddr << 16) | (physCPU << 8) | virtCPU;
		int bufferSize = 0x10;
		int arg2 = (verNumber << 16) | bufferSize;
		int buildDate = 0x1000;
		int arg3 = buildDate;
		var data = "my.spinnaker".getBytes("ASCII");

		var versionData = packVersionData(arg1, arg2, arg3, data);

		assertThrows(IllegalArgumentException.class, () -> {
			new VersionInfo(versionData, false);
		});
	}

	@Test
	void testInvalidSizedVersionData() throws UnsupportedEncodingException {
		int p2pAddr = 0xf0a1;
		int physCPU = 0xff;
		int virtCPU = 0x0b;
		int verNumber = 0xff;
		int arg1 = (p2pAddr << 16) | (physCPU << 8) | virtCPU;
		int bufferSize = 0x10;
		int arg2 = ((verNumber << 16) | bufferSize);
		// int build_date = 0x1000;
		// int arg3 = build_date;
		var data = "my/spinnaker".getBytes("ASCII");

		// Oh arg3, where art thou?
		var versionData = allocate(21).order(LITTLE_ENDIAN);
		versionData.putInt(arg1).putInt(arg2)/*.putInt(arg3)*/.put(data).flip();

		assertThrows(IllegalArgumentException.class, () -> {
			new VersionInfo(versionData, false);
		});
	}
}
