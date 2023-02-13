/*
 * Copyright (c) 2018-2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.sdp;

/** SDP port handling output buffering data streaming. */
public enum SDPPort {
	/** Default port. */
	DEFAULT_PORT(0),
	/** Command port for the buffered in functionality. */
	INPUT_BUFFERING_SDP_PORT(1),
	/** Command port for the buffered out functionality. */
	OUTPUT_BUFFERING_SDP_PORT(2),
	/** Command port for resetting runtime, etc. */
	RUNNING_COMMAND_SDP_PORT(3),
	/** Extra monitor core re injection functionality. */
	EXTRA_MONITOR_CORE_REINJECTION(4),
	/** Extra monitor core data transfer functionality. */
	EXTRA_MONITOR_CORE_DATA_SPEED_UP(5),
	/** Messages directed at the packet gatherer for the speed up protocols. */
	GATHERER_DATA_SPEED_UP(6);

	/** The port ID. */
	public final int value;

	SDPPort(int value) {
		this.value = value;
	}
}
