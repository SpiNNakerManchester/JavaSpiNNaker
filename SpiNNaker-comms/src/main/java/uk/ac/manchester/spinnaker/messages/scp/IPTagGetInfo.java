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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.lang.Byte.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.model.IPTagCommand.TTO;
import static uk.ac.manchester.spinnaker.messages.scp.IPTagFieldDefinitions.COMMAND_FIELD;
import static uk.ac.manchester.spinnaker.messages.scp.SCPCommand.CMD_IPTAG;

import java.nio.ByteBuffer;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.messages.model.IPTagTimeOutWaitTime;
import uk.ac.manchester.spinnaker.messages.model.UnexpectedResponseCodeException;

/**
 * An SCP Request information about IP tags. The response payload is the
 * {@linkplain TagInfo tag <em>system</em> information}.
 * <p>
 * Handled by {@code cmd_iptag()} in {@code scamp-cmd.c} (or {@code bmp_cmd.c},
 * if sent to a BMP).
 *
 * @see IPTagGet
 */
public class IPTagGetInfo extends SCPRequest<IPTagGetInfo.Response> {
	private static final int IPTAG_MAX = 255;

	/**
	 * @param chip
	 *            The chip to query for information.
	 */
	public IPTagGetInfo(HasChipLocation chip) {
		super(chip.getScampCore(), CMD_IPTAG, TTO.value << COMMAND_FIELD,
				IPTAG_MAX);
	}

	@Override
	public Response getSCPResponse(ByteBuffer buffer)
			throws UnexpectedResponseCodeException {
		return new IPTagGetInfo.Response(buffer);
	}

	public static final class TagInfo {
		/**
		 * The timeout for transient IP tags (i.e., responses to SCP commands).
		 */
		public final IPTagTimeOutWaitTime transientTimeout;

		/** The count of the IP tag pool size. */
		public final int poolSize;

		/** The count of the number of fixed IP tag entries. */
		public final int fixedSize;

		private TagInfo(ByteBuffer buffer) {
			transientTimeout = IPTagTimeOutWaitTime.get(buffer.get());
			buffer.get(); // skip 1 (sizeof(iptag_t) isn't relevant to us)
			poolSize = toUnsignedInt(buffer.get());
			fixedSize = toUnsignedInt(buffer.get());
		}
	}

	/** An SCP response to a request for information about IP tags. */
	public static final class Response
			extends PayloadedResponse<TagInfo, RuntimeException> {
		Response(ByteBuffer buffer) throws UnexpectedResponseCodeException {
			super("Get IP Tag Info", CMD_IPTAG, buffer);
		}

		@Override
		protected TagInfo parse(ByteBuffer buffer) throws RuntimeException {
			return new TagInfo(buffer);
		}
	}
}
