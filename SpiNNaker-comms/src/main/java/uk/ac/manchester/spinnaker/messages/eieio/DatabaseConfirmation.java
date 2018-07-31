package uk.ac.manchester.spinnaker.messages.eieio;

import static java.nio.charset.Charset.defaultCharset;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.DATABASE_CONFIRMATION;

import java.nio.ByteBuffer;

/**
 * Packet which contains the path to the database created by the toolchain which
 * is to be used by any software which interfaces with SpiNNaker.
 */
public class DatabaseConfirmation extends EIEIOCommandMessage {
	public final String databasePath;

	public DatabaseConfirmation() {
		super(DATABASE_CONFIRMATION);
		databasePath = null;
	}

	public DatabaseConfirmation(String databasePath) {
		super(DATABASE_CONFIRMATION);
		this.databasePath = databasePath;
	}

	public DatabaseConfirmation(EIEIOCommandHeader header, ByteBuffer data) {
		super(header);
		if (data.remaining() > 0) {
			databasePath = new String(data.array(), data.position(),
					data.remaining(), defaultCharset());
		} else {
			databasePath = null;
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		if (databasePath != null) {
			buffer.put(databasePath.getBytes(defaultCharset()));
		}
	}
}
