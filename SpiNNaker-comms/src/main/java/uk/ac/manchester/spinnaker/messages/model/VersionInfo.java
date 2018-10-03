package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Byte.toUnsignedInt;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

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
		Matcher m = VERSION_RE.matcher(versionString);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"incorrect version format: " + versionString);
		}
		return new Version(m.group("major"), m.group("minor"),
				m.group("revision"));
	}

	/**
	 * @param buffer
	 *            buffer holding an SCP packet containing version information
	 */
	public VersionInfo(ByteBuffer buffer) {
		int p = toUnsignedInt(buffer.get());
		physicalCPUID = toUnsignedInt(buffer.get());
		int y = toUnsignedInt(buffer.get());
		int x = toUnsignedInt(buffer.get());
		core = new CoreLocation(x, y, p);
		buffer.getShort(); // Ignore 2 bytes
		int vn = Short.toUnsignedInt(buffer.getShort());
		buildDate = buffer.getInt();

		String decoded = new String(buffer.array(), buffer.position(),
				buffer.remaining(), UTF_8);
		String original = decoded;
		if (vn < MAGIC_VERSION) {
			versionString = decoded;
			versionNumber = new Version(vn / H, vn % H, 0);
		} else {
			String[] bits = decoded.split(NUL, FULL_BITS);
			if (bits.length < NAME_BITS || bits.length > FULL_BITS) {
				throw new IllegalArgumentException(
						"incorrect version format: " + original);
			}
			decoded = bits[0];
			versionString = bits[1];
			versionNumber = parseVersionString(versionString);
		}

		String[] bits = decoded.split("/", NAME_BITS);
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
}
