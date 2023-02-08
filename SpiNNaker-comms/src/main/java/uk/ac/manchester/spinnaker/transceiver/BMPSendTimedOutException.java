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
import static uk.ac.manchester.spinnaker.utils.UnitConstants.MSEC_PER_SEC;

import java.net.SocketTimeoutException;

import uk.ac.manchester.spinnaker.messages.scp.SCPRequestHeader;

/**
 * Indicates that message sending to a BMP timed out.
 */
public final class BMPSendTimedOutException
		extends SocketTimeoutException {
	private static final long serialVersionUID = 1660563278795501381L;

	BMPSendTimedOutException(SCPRequestHeader hdr, int timeout) {
		super(format("Operation %s timed out after %f seconds", hdr.command,
				timeout / (double) MSEC_PER_SEC));
	}
}
