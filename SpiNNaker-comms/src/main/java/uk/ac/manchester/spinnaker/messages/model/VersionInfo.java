package uk.ac.manchester.spinnaker.messages.model;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/** Decodes SC&MP/SARK version information as returned by the SVER command. */
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
			.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

	/**
	 * @param buffer
	 *            buffer holding an SCP packet containing version information
	 */
	public VersionInfo(ByteBuffer buffer) {
		byte p = buffer.get();
		physicalCPUID = buffer.get() & 0xFF;
		byte y = buffer.get();
		byte x = buffer.get();
		core = new CoreLocation(x & 0xFF, y & 0xFF, p & 0xFF);
		buffer.getShort(); // Ignore 2 byes
		int vn = buffer.getShort() & 0xFFFF;
		buildDate = buffer.getInt();
		String vd = new String(buffer.array(), buffer.position(),
				buffer.remaining(), UTF_8);
		String[] bits;
		if (vn < 0xFFFF) {
			versionString = vd;
			versionNumber = new Version(vn / 100, vn % 100, 0);
			bits = vd.split("/", 2);
		} else {
			bits = vd.split("\\|0", 2);
			versionString = bits[1];
			Matcher m = VERSION_RE.matcher(bits[1]);
			if (!m.matches()) {
				throw new IllegalArgumentException(
						"incorrect version format: " + bits[1]);
			}
			versionNumber = new Version(m.group(1), m.group(2), m.group(3));
			bits = bits[0].replaceFirst("[|0]+$", "").split("/", 2);
		}
		name = bits[0];
		hardware = bits[1];
	}
}