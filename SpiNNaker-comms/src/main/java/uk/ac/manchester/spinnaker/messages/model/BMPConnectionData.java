package uk.ac.manchester.spinnaker.messages.model;

import java.net.InetAddress;
import static java.net.InetAddress.getByAddress;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.net.UnknownHostException;
import java.util.Collection;

/**
 * Contains the details of a connection to a SpiNNaker Board Management
 * Processor (BMP).
 */
public class BMPConnectionData {
	/** The boards to be addressed. */
	public final Collection<Integer> boards;
	/** The cabinet number. */
	public final int cabinet;
	/** The frame number. Frames are contained within a cabinet. */
	public final int frame;
	/** The IP address of the BMP. */
	public final InetAddress ipAddress;
	/**
	 * The port number associated with the BMP connection, or <tt>null</tt> for
	 * the default.
	 */
	public final Integer portNumber;

	/**
	 * @param cabinet The number of the cabinet containing the frame.
	 * @param frame The number of the frame containing the boards.
	 * @param ipAddress The address of the BMP.
	 * @param boards The boards controlled by the BMP.
	 * @param portNumber The BMP's port.
	 */
	public BMPConnectionData(int cabinet, int frame, InetAddress ipAddress,
			Collection<Integer> boards, Integer portNumber) {
		this.cabinet = cabinet;
		this.frame = frame;
		this.ipAddress = ipAddress;
		this.boards = boards;
		this.portNumber = portNumber;
	}

	private static final int MIN_BYTE_FIELD = 3;

	/**
	 * Work out the BMP connection IP address given the machine details. This is
	 * assumed to be the IP address of the machine, with 1 subtracted from the
	 * final part e.g. if the machine IP address is 192.168.0.5, the BMP IP
	 * address is assumed to be 192.168.0.4
	 *
	 * @param host
	 *            the SpiNNaker machine main host
	 * @param numBoards
	 *            the number of boards in the machine
	 * @throws UnknownHostException
	 *             If the IP address computations fail
	 */
	public BMPConnectionData(InetAddress host, int numBoards)
			throws UnknownHostException {
		// take the IP address, split by dots, and subtract 1 off last bit
		byte[] ipBits = host.getAddress();
		ipBits[MIN_BYTE_FIELD]--;
		ipAddress = getByAddress(ipBits);
		portNumber = SCP_SCAMP_PORT;

		// Assume a single board with no cabinet or frame specified
		cabinet = 0;
		frame = 0;

		// add board scope for each split
		// if null, the end user didn't enter anything, so assume one board
		// starting at position 0
		if (numBoards == 0) {
			boards = singletonList(0);
		} else {
			boards = range(0, numBoards).boxed().collect(toList());
		}
	}
}
