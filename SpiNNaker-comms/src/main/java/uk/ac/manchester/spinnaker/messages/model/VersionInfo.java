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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Byte.toUnsignedInt;
import static java.lang.Short.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.bmp.BMPLocation;

/**
 * Decodes SC&amp;MP/SARK version information as returned by the SVER command.
 */
public final class VersionInfo {
	/** The build date of the software, in seconds since 1st January 1970. */
	public final int buildDate;

	/** The hardware being run on. */
	public final String hardware;

	/** The name of the software. */
	public final String name;

	/**
	 * The physical CPU ID. Note that this is only really useful for debugging,
	 * as core IDs are remapped by SCAMP so that SCAMP is always running on
	 * virtual core zero.
	 */
	public final int physicalCPUID;

	/** The version number of the software. */
	public final Version versionNumber;

	/** The version information as text. */
	public final String versionString;

	/** The location of the core where the information was obtained. */
	public final HasCoreLocation core;

	private static final Pattern VERSION_RE = Pattern
			.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<revision>\\d+)");

	private static final String NUL = "\u0000";

	private static Version parseVersionString(String versionString) {
		var m = VERSION_RE.matcher(versionString);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"incorrect version format: " + versionString);
		}
		return new Version(m.group("major"), m.group("minor"),
				m.group("revision"));
	}

	private static String decodeFromBuffer(ByteBuffer buffer) {
		if (buffer.hasArray()) {
			return new String(buffer.array(),
					buffer.arrayOffset() + buffer.position(),
					buffer.remaining(), UTF_8);
		} else {
			byte[] tmp = new byte[buffer.remaining()];
			buffer.get(tmp);
			return new String(tmp, UTF_8);
		}
	}

	/**
	 * @param buffer
	 *            buffer holding an SCP packet containing version information
	 * @param isBMP
	 *            Are we really processing the result of a request to get a
	 *            BMP's version.
	 * @throws IllegalArgumentException
	 *             If the buffer contains an unsupported format of data
	 */
	public VersionInfo(ByteBuffer buffer, boolean isBMP) {
		int p = toUnsignedInt(buffer.get());
		physicalCPUID = toUnsignedInt(buffer.get());
		int y = toUnsignedInt(buffer.get());
		int x = toUnsignedInt(buffer.get());
		if (isBMP) {
			core = new BMPLocation(x, y, p);
		} else {
			core = new CoreLocation(x, y, p);
		}
		buffer.getShort(); // Ignore 2 bytes
		int vn = toUnsignedInt(buffer.getShort());
		buildDate = buffer.getInt();

		var decoded = decodeFromBuffer(buffer);
		var original = decoded;
		if (vn < MAGIC_VERSION) {
			versionString = decoded;
			versionNumber = new Version(vn / H, vn % H, 0);
		} else {
			var bits = decoded.split(NUL, FULL_BITS);
			if (bits.length < NAME_BITS || bits.length > FULL_BITS) {
				throw new IllegalArgumentException(
						"incorrect version format: " + original);
			}
			decoded = bits[0];
			versionString = bits[1];
			versionNumber = parseVersionString(versionString);
		}

		var bits = decoded.split("/", NAME_BITS);
		if (bits.length != NAME_BITS) {
			throw new IllegalArgumentException(
					"incorrect version format: " + original);
		}
		name = bits[0];
		hardware = bits[1];
	}

	private static final int H = 100;

	private static final int FULL_BITS = 3;

	private static final int NAME_BITS = 2;

	private static final int MAGIC_VERSION = 0xFFFF;

	@Override
	public String toString() {
		return "VersionInfo(" + core + " (phys:" + physicalCPUID
				+ "), version: " + versionNumber + ", " + name + "/" + hardware
				+ ", " + ofInstant(ofEpochSecond(buildDate, 0), UTC)
						.format(ISO_INSTANT)
				+ ")";
	}
}
