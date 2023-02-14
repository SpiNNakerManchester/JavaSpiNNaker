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

/**
 * Various constants used in the flood fill sub-protocol.
 *
 * @author Donal Fellows
 */
abstract class FloodFillConstants {
	private FloodFillConstants() {
	}

	/** Send on all links. */
	static final int FORWARD_LINKS = 0x3F;

	/** Inter-send delay 24&mu;s. */
	static final int DELAY = 0x18;

	/** Number of times to resend a data message. */
	static final int DATA_RESEND = 2;

	/** Initial level. (What is level?) */
	static final int INIT_LEVEL = 3;

	/** Whether to issue an ID for the fill. */
	static final int ADD_ID = 1;
}
