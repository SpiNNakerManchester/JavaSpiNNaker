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
package uk.ac.manchester.spinnaker.transceiver;

import java.io.IOException;

import uk.ac.manchester.spinnaker.connections.ConnectionSelector;
import uk.ac.manchester.spinnaker.connections.SCPConnection;
import uk.ac.manchester.spinnaker.machine.CoreSubsets;
import uk.ac.manchester.spinnaker.messages.model.AppID;
import uk.ac.manchester.spinnaker.messages.scp.ApplicationRun;

/** Launch an application. */
class ApplicationRunProcess extends TxrxProcess {
	/**
	 * Create.
	 *
	 * @param connectionSelector
	 *            How to choose where to send messages.
	 * @param retryTracker
	 *            Object used to track how many retries were used in an
	 *            operation. May be {@code null} if no suck tracking is
	 *            required.
	 */
	ApplicationRunProcess(
			ConnectionSelector<? extends SCPConnection> connectionSelector,
			RetryTracker retryTracker) {
		super(connectionSelector, retryTracker);
	}

	/**
	 * Launch an application (already loaded).
	 *
	 * @param appID
	 *            The application ID to launch.
	 * @param coreSubsets
	 *            Which cores to launch.
	 * @param wait
	 *            Whether to wait for the application launch to fully complete
	 *            before returning.
	 * @throws IOException
	 *             If anything goes wrong with networking.
	 * @throws ProcessException
	 *             If SpiNNaker rejects a message.
	 * @throws InterruptedException
	 *             If the communications were interrupted.
	 */
	void run(AppID appID, CoreSubsets coreSubsets, boolean wait)
			throws ProcessException, IOException, InterruptedException {
		for (var chip : coreSubsets.getChips()) {
			sendRequest(new ApplicationRun(appID, chip,
					coreSubsets.pByChip(chip), wait));
		}
		finishBatch();
	}
}
