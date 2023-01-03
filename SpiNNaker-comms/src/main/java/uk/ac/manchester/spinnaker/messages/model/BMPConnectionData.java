/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.String.format;
import static java.net.InetAddress.getByAddress;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.IntStream.range;
import static uk.ac.manchester.spinnaker.messages.Constants.SCP_SCAMP_PORT;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;

import jakarta.validation.Valid;
import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;
import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/**
 * Contains the details of a connection to a SpiNNaker Board Management
 * Processor (BMP).
 */
public class BMPConnectionData {
	/** The boards to be addressed. Unmodifiable. */
	public final Collection<@Valid BMPBoard> boards;

	/** The coordinates of the BMP that manages a set of boards. */
	@Valid
	public final BMPCoords bmp;

	/** The IP address of the BMP. */
	@IPAddress
	public final InetAddress ipAddress;

	/**
	 * The port number associated with the BMP connection, or {@code null} for
	 * the default.
	 */
	@UDPPort
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
		this.bmp = new BMPCoords(cabinet, frame);
		this.ipAddress = ipAddress;
		this.boards = boards.stream().map(BMPBoard::new)
				.collect(toUnmodifiableList());
		this.portNumber = portNumber;
	}

	/**
	 * @param coords The coordinates of the BMP.
	 * @param ipAddress The address of the BMP.
	 * @param boards The boards controlled by the BMP.
	 * @param portNumber The BMP's port.
	 */
	public BMPConnectionData(BMPCoords coords, InetAddress ipAddress,
			Collection<Integer> boards, Integer portNumber) {
		this.bmp = coords;
		this.ipAddress = ipAddress;
		this.boards = boards.stream().map(BMPBoard::new)
				.collect(toUnmodifiableList());
		this.portNumber = portNumber;
	}

	private static final int MIN_BYTE_FIELD = 3;

	/**
	 * Work out the BMP connection IP address given the machine details. This is
	 * assumed to be the IP address of the machine, with 1 subtracted from the
	 * final part e.g. if the machine IP address is {@code 192.168.0.5}, the BMP
	 * IP address is assumed to be {@code 192.168.0.4}.
	 * <p>
	 * This algorithm is rather hokey. Far better for the user to simply know
	 * where the BMP actually is (this is necessary in any large deployment
	 * anyway).
	 *
	 * @param host
	 *            the SpiNNaker machine main host
	 * @param numBoards
	 *            the number of boards in the machine
	 * @throws UnknownHostException
	 *             If the IP address computations fail
	 * @throws IllegalArgumentException
	 *             If a host with an address that can't be related to a BMP is
	 *             given. Specifically, the given IP address can't end in
	 *             {@code .0} or {@code .1} or things will not work.
	 */
	public BMPConnectionData(InetAddress host, int numBoards)
			throws UnknownHostException {
		// take the IP address, split by dots, and subtract 1 off last bit
		var ipBits = host.getAddress();
		if (ipBits[MIN_BYTE_FIELD] == 0 || ipBits[MIN_BYTE_FIELD] == 1) {
			// Last digit of valid IP address can't really be 0 or 255
			throw new IllegalArgumentException(
					"BMP address would have illegal IP address");
		}
		ipBits[MIN_BYTE_FIELD]--;
		ipAddress = getByAddress(ipBits);
		portNumber = SCP_SCAMP_PORT;

		// Assume a single board with no cabinet or frame specified
		bmp = new BMPCoords(0, 0);

		// add board scope for each split
		// if null, the end user didn't enter anything, so assume one board
		// starting at position 0
		if (numBoards == 0) {
			boards = List.of(new BMPBoard(0));
		} else {
			boards = range(0, numBoards).mapToObj(BMPBoard::new)
					.collect(toUnmodifiableList());
		}
	}

	@Override
	public String toString() {
		return format(
				"(c:%d,f:%d,b:%s; %s)", bmp.cabinet, bmp.frame,
				boards.stream().findFirst()
						.map(board -> board.board + "...").orElse("<empty>"),
				ipAddress);
	}
}
