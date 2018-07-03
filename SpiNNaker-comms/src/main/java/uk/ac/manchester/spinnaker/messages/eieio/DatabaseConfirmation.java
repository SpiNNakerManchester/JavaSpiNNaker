package uk.ac.manchester.spinnaker.messages.eieio;

import static java.nio.charset.Charset.defaultCharset;

import java.nio.ByteBuffer;

/**
 * Packet which contains the path to the database created by the toolchain which
 * is to be used by any software which interfaces with SpiNNaker.
 */
public class DatabaseConfirmation extends EIEIOCommandMessage {
	public final String database_path;

	public DatabaseConfirmation() {
		super(EIEIOCommandID.DATABASE_CONFIRMATION);
		database_path = null;
	}

	public DatabaseConfirmation(String database_path) {
		super(EIEIOCommandID.DATABASE_CONFIRMATION);
		this.database_path = database_path;
	}

	public DatabaseConfirmation(EIEIOCommandHeader header, ByteBuffer data,
			int offset) {
		super(header, data, offset);
		if (data.limit() - offset > 0) {
			database_path = new String(data.array(), offset,
					data.limit() - offset, defaultCharset());
		} else {
			database_path = null;
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		if (database_path != null) {
			buffer.put(database_path.getBytes(defaultCharset()));
		}
	}
}
