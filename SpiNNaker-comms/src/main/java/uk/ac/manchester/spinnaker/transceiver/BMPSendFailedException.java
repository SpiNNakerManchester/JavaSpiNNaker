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

import static java.lang.String.format;
import static uk.ac.manchester.spinnaker.transceiver.BMPCommandProcess.BMP_RETRIES;

import java.io.IOException;
import java.util.List;

import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.messages.scp.SCPRequest;

/**
 * Indicates that message sending to a BMP failed for various reasons.
 */
public final class BMPSendFailedException extends IOException {
	private static final long serialVersionUID = -7806549580351626377L;

	BMPSendFailedException(SCPRequest<?> req, HasCoreLocation core,
			List<String> retryReason) {
		super(format(
				"Errors sending request %s (%s) to %d,%d,%d over %d retries:"
				+ " %s",
				req.scpRequestHeader.command, req, core.getX(), core.getY(),
				core.getP(), BMP_RETRIES, retryReason));
	}
}
