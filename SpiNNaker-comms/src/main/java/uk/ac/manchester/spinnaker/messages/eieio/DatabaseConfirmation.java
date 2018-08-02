package uk.ac.manchester.spinnaker.messages.eieio;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Objects.requireNonNull;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.DATABASE_CONFIRMATION;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Packet which contains the path to the database created by the toolchain which
 * is to be used by any software which interfaces with SpiNNaker.
 */
public class DatabaseConfirmation extends EIEIOCommandMessage {
	/**
	 * The path to the database. Note that there is a length limit; the overall
	 * message must fit in a SpiNNaker UDP message.
	 */
	public final String databasePath;
	/**
	 * The encoding of the database path into bytes.
	 */
	private static final Charset CHARSET = defaultCharset();

	/**
	 * Create a message without a database path in it.
	 */
	public DatabaseConfirmation() {
		super(DATABASE_CONFIRMATION);
		databasePath = null;
	}

	/**
	 * Create a message with a database path in it.
	 *
	 * @param databasePath
	 *            The path.
	 */
	public DatabaseConfirmation(String databasePath) {
		super(DATABASE_CONFIRMATION);
		this.databasePath = requireNonNull(databasePath);
	}

	/**
	 * Deserialise from a buffer.
	 *
	 * @param data
	 *            The buffer to read from
	 */
	DatabaseConfirmation(ByteBuffer data) {
		super(data);
		if (data.remaining() > 0) {
			databasePath = new String(data.array(), data.position(),
					data.remaining(), CHARSET);
		} else {
			databasePath = null;
		}
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		if (databasePath != null) {
			buffer.put(databasePath.getBytes(CHARSET));
		}
	}
}
