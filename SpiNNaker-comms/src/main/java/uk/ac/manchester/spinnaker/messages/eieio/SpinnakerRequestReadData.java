/*
 * Copyright (c) 2018 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.messages.eieio;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.SPINNAKER_REQUEST_READ_DATA;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Message used in the context of the buffering output mechanism which is sent
 * from the SpiNNaker system to the host computer to signal that some data is
 * available to be read.
 */
public class SpinnakerRequestReadData extends EIEIOCommandMessage
		implements HasCoreLocation {
	private final int numRequests;

	private final int sequenceNumber;

	private final HasCoreLocation core;

	private final int[] channel;

	private final int[] regionID;

	private final int[] startAddress;

	private final int[] spaceRead;

	/**
	 * Create a message instance.
	 *
	 * @param core
	 *            The core talked about.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param numRequests
	 *            The expected number of requests.
	 * @param channel
	 *            The channel IDs.
	 * @param regionID
	 *            The region IDs.
	 * @param startAddress
	 *            The start addresses to read from.
	 * @param spaceRead
	 *            The number of bytes to read from each.
	 * @throws IllegalArgumentException
	 *             if the number of channels, regionIDs, addresses and sizes
	 *             don't match the number of requests.
	 */
	public SpinnakerRequestReadData(HasCoreLocation core, int sequenceNum,
			int numRequests, int[] channel, int[] regionID,
			int[] startAddress, int[] spaceRead) {
		super(SPINNAKER_REQUEST_READ_DATA);
		this.core = core;
		this.numRequests = (byte) (numRequests & N_REQUESTS_MASK);
		this.sequenceNumber = sequenceNum;
		if (channel.length != numRequests || regionID.length != numRequests
				|| startAddress.length != numRequests
				|| spaceRead.length != numRequests) {
			throw new IllegalArgumentException(
					"lengths of channel array, region ID array, "
							+ "start address array, and space read array "
							+ "must all match the number of requests");
		}
		this.channel = channel;
		this.regionID = regionID;
		this.startAddress = startAddress;
		this.spaceRead = spaceRead;
	}

	/**
	 * Create a message instance about a single move.
	 *
	 * @param core
	 *            The core talked about.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param channel
	 *            The channel ID.
	 * @param regionID
	 *            The region ID.
	 * @param startAddress
	 *            The start address to read from.
	 * @param spaceRead
	 *            The number of bytes to read.
	 */
	public SpinnakerRequestReadData(HasCoreLocation core, int sequenceNum,
			int channel, int regionID, int startAddress, int spaceRead) {
		this(core, sequenceNum, (byte) 1, new int[] {
			channel
		}, new int[] {
			regionID
		}, new int[] {
			startAddress
		}, new int[] {
			spaceRead
		});
	}

	private static final int CORE_SHIFT = 3;

	private static final int N_REQUESTS_MASK = (1 << CORE_SHIFT) - 1;

	/**
	 * Deserialise.
	 *
	 * @param data
	 *            the data buffer.
	 */
	SpinnakerRequestReadData(ByteBuffer data) {
		super(data);

		int x = toUnsignedInt(data.get());
		int y = toUnsignedInt(data.get());
		int pr = toUnsignedInt(data.get());
		this.sequenceNumber = toUnsignedInt(data.get());

		byte p = (byte) (pr >>> CORE_SHIFT);
		int n = pr & N_REQUESTS_MASK;
		this.core = new CoreLocation(x, y, p);
		this.numRequests = (byte) n;

		channel = new int[n];
		regionID = new int[n];
		startAddress = new int[n];
		spaceRead = new int[n];
		for (int i = 0; i < n; i++) {
			if (i != 0) {
				// Skip two bytes
				data.getShort();
			}
			channel[i] = toUnsignedInt(data.get());
			regionID[i] = toUnsignedInt(data.get());
			startAddress[i] = data.getInt();
			spaceRead[i] = data.getInt();
		}
	}

	@Override
	public int getX() {
		return core.getX();
	}

	@Override
	public int getY() {
		return core.getY();
	}

	@Override
	public int getP() {
		return core.getP();
	}

	/** @return The number of requests in the message. */
	public int getNumRequests() {
		return numRequests;
	}

	/** @return The sequence number of the message. */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Get the channel ID.
	 *
	 * @param ackID
	 *            Which acknowledgement.
	 * @return The channel ID.
	 */
	public int getChannel(int ackID) {
		return channel[ackID];
	}

	/**
	 * Get the region ID.
	 *
	 * @param ackID
	 *            Which acknowledgement.
	 * @return The region ID.
	 */
	public int getRegionID(int ackID) {
		return regionID[ackID];
	}

	/**
	 * Get the start addresses to read from.
	 *
	 * @param ackID
	 *            Which acknowledgement.
	 * @return The start addresses to read from.
	 */
	public int getStartAddress(int ackID) {
		return startAddress[ackID];
	}

	/**
	 * Get the number of bytes to read.
	 *
	 * @param ackID
	 *            Which acknowledgement.
	 * @return The number of bytes to read.
	 */
	public int getSpaceRead(int ackID) {
		return spaceRead[ackID];
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put((byte) core.getX());
		buffer.put((byte) core.getY());

		for (int i = 0; i < numRequests; i++) {
			if (i == 0) {
				buffer.put((byte) (core.getP() << CORE_SHIFT | numRequests));
				buffer.put((byte) sequenceNumber);
			} else {
				buffer.putShort((short) 0);
			}

			buffer.put((byte) channel[i]);
			buffer.put((byte) regionID[i]);
			buffer.putInt(startAddress[i]);
			buffer.putInt(spaceRead[i]);
		}
	}
}
