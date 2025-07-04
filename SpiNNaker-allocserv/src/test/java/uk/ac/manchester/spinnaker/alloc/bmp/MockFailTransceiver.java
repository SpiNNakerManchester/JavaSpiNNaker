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

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import uk.ac.manchester.spinnaker.messages.model.BMPConnectionData;
import uk.ac.manchester.spinnaker.messages.model.Blacklist;
import uk.ac.manchester.spinnaker.transceiver.UnimplementedBMPTransceiver;
import uk.ac.manchester.spinnaker.utils.ValueHolder;

/**
 * BMP transceiver mock just for test purposes.  This one always fails.
 */
public class MockFailTransceiver extends UnimplementedBMPTransceiver {
	private static final Logger log = getLogger(MockTransceiver.class);

	/**
	 * Install this mock transceiver as the thing to be manufactured by the
	 * given transceiver factory.
	 *
	 * @param txrxFactory
	 *            The transceiver factory to install into.
	 */
	@SuppressWarnings("deprecation")
	public static void installIntoFactory(TransceiverFactory txrxFactory) {
		txrxFactory.getTestAPI().setFactory(MockFailTransceiver::new);
	}

	private MockFailTransceiver(String machineName, BMPConnectionData data,
			ValueHolder<Blacklist> setBlacklist) {
		log.info("constructed dummy fail transceiver for {} ({} : {})",
				machineName, data.ipAddress, data.boards);
	}
}
