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
import static uk.ac.manchester.spinnaker.messages.eieio.EIEIOCommandID.SPINNAKER_REQUEST_BUFFERS;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;

/**
 * Message used in the context of the buffering input mechanism which is sent by
 * the SpiNNaker system to the host computer to ask for more data to inject
 * during the simulation.
 */
public class SpinnakerRequestBuffers extends EIEIOCommandMessage
		implements HasCoreLocation {
	/** What core are we talking about. */
	public final HasCoreLocation core;

	/** What region of the core's memory. */
	public final int regionID;

	/** The message sequence number. */
	public final int sequenceNum;

	/** How much space is available. */
	public final int spaceAvailable;

	/**
	 * Create an instance.
	 *
	 * @param core
	 *            The core being talked about.
	 * @param regionID
	 *            The memory region being talked about.
	 * @param sequenceNum
	 *            The message sequence number.
	 * @param spaceAvailable
	 *            The space available, in bytes.
	 */
	public SpinnakerRequestBuffers(HasCoreLocation core, byte regionID,
			byte sequenceNum, int spaceAvailable) {
		super(SPINNAKER_REQUEST_BUFFERS);
		this.core = core;
		this.regionID = regionID;
		this.sequenceNum = sequenceNum;
		this.spaceAvailable = spaceAvailable;
	}

	private static final int PROC_SHIFT = 3;

	private static final int PROC_MASK = 0b00011111;

	private static final int REGION_MASK = 0b00001111;

	SpinnakerRequestBuffers(ByteBuffer data) {
		super(data);

		int y = toUnsignedInt(data.get());
		int x = toUnsignedInt(data.get());
		int p = ((data.get() >>> PROC_SHIFT) & PROC_MASK);
		core = new CoreLocation(x, y, p);
		data.get(); // ignore
		regionID = data.get() & REGION_MASK;
		sequenceNum = toUnsignedInt(data.get());
		spaceAvailable = data.getInt();
	}

	@Override
	public void addToBuffer(ByteBuffer buffer) {
		super.addToBuffer(buffer);
		buffer.put((byte) core.getY());
		buffer.put((byte) core.getX());
		buffer.put((byte) ((core.getP() & PROC_MASK) << PROC_SHIFT));
		buffer.put((byte) 0);
		buffer.put((byte) regionID);
		buffer.put((byte) sequenceNum);
		buffer.putInt(spaceAvailable);
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
}
