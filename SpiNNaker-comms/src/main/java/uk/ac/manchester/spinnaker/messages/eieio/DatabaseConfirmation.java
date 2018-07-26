package uk.ac.manchester.spinnaker.messages.eieio;

import static java.nio.charset.Charset.defaultCharset;

import java.nio.ByteBuffer;

/**
 * Packet which contains the path to the database created by the toolchain which
 * is to be used by any software which interfaces with SpiNNaker.
 */
public class DatabaseConfirmation extends EIEIOCommandMessage {
	public final String databasePath;

	public DatabaseConfirmation() {
		super(EIEIOCommandID.DATABASE_CONFIRMATION);
		databasePath = null;
	}

	public DatabaseConfirmation(String databasePath) {
		super(EIEIOCommandID.DATABASE_CONFIRMATION);
		this.databasePath = databasePath;
	}

	public DatabaseConfirmation(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		if (data.limit() - offset > 0) {
			databasePath = new String(data.array(), offset,
					data.limit() - offset, defaultCharset());
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
