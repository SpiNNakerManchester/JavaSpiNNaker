package uk.ac.manchester.spinnaker.connections;

import java.util.Collection;

/**
 * Contains the details of a connection to a SpiNNaker Board Management
 * Processor (BMP).
 */
class BMPConnectionData {
	/** The boards to be addressed. */
	public final Collection<Integer> boards;
	/** The cabinet number. */
	public final int cabinet;
	/** The frame number. Frames are contained within a cabinet. */
	public final int frame;
	/** The IP address or host name of the BMP. */
	public final String ipAddress;
	/**
	 * The port number associated with the BMP connection, or <tt>null</tt> for
	 * the default.
	 */
	public final Integer portNumber;

	public BMPConnectionData(int cabinet, int frame, String ipAddress,
			Collection<Integer> boards, Integer portNumber) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.ipAddress = ipAddress;
		this.boards = boards;
		this.portNumber = portNumber;
	}
}
