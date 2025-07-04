/*
 * Copyright (c) 2025 The University of Manchester
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
package uk.ac.manchester.spinnaker.alloc.bmp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.manchester.spinnaker.machine.board.BMPBoard;
import uk.ac.manchester.spinnaker.messages.model.ADCInfo;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.ProcessException;

/**
 * Dummy implementation of the SpiNNakerControl interface.
 */
public class SpiNNakerControlDummy implements SpiNNakerControl {

	private static final String DUMMY_SERIAL = "00000000";

	private final Map<BMPBoard, Blacklist> blacklists = new HashMap<>();

	@Override
	public void powerOnAndCheck(List<BMPBoard> boards) throws ProcessException,
			InterruptedException, IOException {
		// Do Nothing
	}

	@Override
	public void setLinkOff(Link link) throws ProcessException, IOException,
			InterruptedException {
		// Do Nothing
	}

	@Override
	public void powerOff(Collection<BMPBoard> boards) throws ProcessException,
			InterruptedException, IOException {
		// Do Nothing

	}

	@Override
	public String readSerial(BMPBoard board) throws ProcessException,
			InterruptedException, IOException {
		return DUMMY_SERIAL;
	}

	@Override
	public ADCInfo readTemp(BMPBoard board) throws ProcessException,
			IOException, InterruptedException {
		return new ADCInfo(ByteBuffer.wrap(new byte[ADCInfo.SIZE]));
	}

	@Override
	public Blacklist readBlacklist(BMPBoard board) throws ProcessException,
			InterruptedException, IOException {
		if (blacklists.containsKey(board)) {
			return blacklists.get(board);
		}
		return new Blacklist(ByteBuffer.wrap(new byte[Integer.BYTES]));
	}

	@Override
	public void writeBlacklist(BMPBoard board, Blacklist blacklist)
			throws ProcessException, InterruptedException, IOException {
		blacklists.put(board, blacklist);
	}

	@Override
	public void ping(List<BMPBoard> boards) {
		// Do Nothing
	}

}
