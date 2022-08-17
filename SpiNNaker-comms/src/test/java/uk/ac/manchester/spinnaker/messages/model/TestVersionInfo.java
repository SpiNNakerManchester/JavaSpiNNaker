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
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import uk.ac.manchester.spinnaker.machine.CoreLocation;

class TestVersionInfo {
	private ByteBuffer packVersionData(int arg1, int arg2, int arg3,
			byte[] data) {
		ByteBuffer buffer = ByteBuffer.allocate(25).order(LITTLE_ENDIAN);
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
		byte[] data = "my/spinnaker".getBytes("ASCII");

		ByteBuffer versionData = packVersionData(arg1, arg2, arg3, data);

		VersionInfo vi = new VersionInfo(versionData, false);
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
		byte[] data = "my.spinnaker".getBytes("ASCII");

		ByteBuffer versionData = packVersionData(arg1, arg2, arg3, data);

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
		byte[] data = "my/spinnaker".getBytes("ASCII");

		// Oh arg3, where art thou?
		ByteBuffer versionData = ByteBuffer.allocate(21).order(LITTLE_ENDIAN);
		versionData.putInt(arg1).putInt(arg2)/* .putInt(arg3) */.put(data)
				.flip();

		assertThrows(IllegalArgumentException.class, () -> {
			new VersionInfo(versionData, false);
		});
	}
}
