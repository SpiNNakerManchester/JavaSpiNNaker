/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import javax.validation.Valid;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.machine.board.BMPCoords;
import uk.ac.manchester.spinnaker.utils.validation.IPAddress;
import uk.ac.manchester.spinnaker.utils.validation.UDPPort;

/**
 * Contains the details of a connection to a SpiNNaker Board Management
 * Processor (BMP).
 *
 * @param boards
 *            The boards to be addressed.
 * @param bmp
 *            The coordinates of the BMP that manages a set of boards.
 * @param ipAddress
 *            The IP address of the BMP.
 * @param portNumber
 *            The port number associated with the BMP connection, or
 *            {@code null} for the default.
 */
public record BMPConnectionData(Collection<@Valid BMPBoard> boards,
		@Valid BMPCoords bmp, @IPAddress InetAddress ipAddress,
		@UDPPort Integer portNumber) {
	/**
	 * @param cabinet
	 *            The number of the cabinet containing the frame.
	 * @param frame
	 *            The number of the frame containing the boards.
	 * @param ipAddress
	 *            The address of the BMP.
	 * @param boards
	 *            The boards controlled by the BMP.
	 * @param portNumber
	 *            The BMP's port.
	 */
	public BMPConnectionData(int cabinet, int frame, InetAddress ipAddress,
			Collection<Integer> boards, Integer portNumber) {
		this(boards.stream().map(BMPBoard::new).collect(toUnmodifiableList()),
				new BMPCoords(cabinet, frame), ipAddress, portNumber);
	}

	/**
	 * @param coords
	 *            The coordinates of the BMP.
	 * @param ipAddress
	 *            The address of the BMP.
	 * @param boards
	 *            The boards controlled by the BMP.
	 * @param portNumber
	 *            The BMP's port.
	 */
	public BMPConnectionData(BMPCoords coords, InetAddress ipAddress,
			Collection<Integer> boards, Integer portNumber) {
		this(boards.stream().map(BMPBoard::new).collect(toUnmodifiableList()),
				coords, ipAddress, portNumber);
	}

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
		/*
		 * Assumes a single board (or small group) with no cabinet or frame
		 * specified
		 */
		this(makeBoards(numBoards), new BMPCoords(0, 0), guessBMPIP(host),
				SCP_SCAMP_PORT);
	}

	private static final int MIN_BYTE_FIELD = 3;

	private static InetAddress guessBMPIP(InetAddress host)
			throws UnknownHostException {
		// take the IP address, split by dots, and subtract 1 off last bit
		var ipBits = host.getAddress();
		if (ipBits[MIN_BYTE_FIELD] == 0 || ipBits[MIN_BYTE_FIELD] == 1) {
			// Last digit of valid IP address can't really be 0 or 255
			throw new IllegalArgumentException(
					"BMP address would have illegal IP address");
		}
		ipBits[MIN_BYTE_FIELD]--;
		return getByAddress(ipBits);
	}

	private static List<BMPBoard> makeBoards(int numBoards) {
		// if 0 or crazy, the end user didn't enter anything useful, so
		// assume one board starting at position 0
		if (numBoards < 1) {
			return List.of(new BMPBoard(0));
		} else {
			return range(0, numBoards).mapToObj(BMPBoard::new)
					.collect(toUnmodifiableList());
		}
	}

	@Override
	public String toString() {
		return format(
				"(c:%d,f:%d,b:%s; %s)", bmp.cabinet(), bmp.frame(),
				boards.stream().findFirst()
						.map(board -> board.board() + "...").orElse("<empty>"),
				ipAddress);
	}
}
