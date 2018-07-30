package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
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
	public String hardware;
	/** The name of the software. */
	public String name;
	public final int physicalCPUID;
	/** The version number of the software. */
	public Version versionNumber;
	/** The version information as text. */
	public String versionString;
	/** The location of the core where the information was obtained. */
	public final HasCoreLocation core;

	private static final Charset UTF_8 = Charset.forName("UTF-8");
	private static final Pattern VERSION_RE = Pattern
			.compile("^(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<revision>\\d+)$");

	/**
	 * @param buffer
	 *            buffer holding an SCP packet containing version information
	 */
	public VersionInfo(ByteBuffer buffer) {
		int p = Byte.toUnsignedInt(buffer.get());
		physicalCPUID = Byte.toUnsignedInt(buffer.get());
		int y = Byte.toUnsignedInt(buffer.get());
		int x = Byte.toUnsignedInt(buffer.get());
		core = new CoreLocation(x, y, p);
		buffer.getShort(); // Ignore 2 byes
		int vn = Short.toUnsignedInt(buffer.getShort());
		buildDate = buffer.getInt();
		String vd = new String(buffer.array(), buffer.position(),
				buffer.remaining(), UTF_8);
		decodeVersion(vn, vd);
	}

	private static final int H = 100;
	private static final int NBITS = 2;
	private static final int MAGIC_VERSION = 0xFFFF;

	private void decodeVersion(int vn, String vd) {
		String nh = vd;
		if (vn < MAGIC_VERSION) {
			versionString = vd;
			versionNumber = new Version(vn / H, vn % H, 0);
		} else {
			nh = uglyDecode(vd.split("\\|0", NBITS));
		}

		String[] bits = nh.split("/", NBITS);
		name = bits[0];
		hardware = bits[1];
	}

	private String uglyDecode(String[] bits) {
		versionString = bits[1];
		Matcher m = VERSION_RE.matcher(versionString);
		if (!m.matches()) {
			throw new IllegalArgumentException(
					"incorrect version format: " + versionString);
		}
		versionNumber = new Version(m.group("major"), m.group("minor"),
				m.group("revision"));
		return bits[0].replaceFirst("[|0]+$", "");
	}
}
