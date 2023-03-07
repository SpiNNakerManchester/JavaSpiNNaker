/*
 * Copyright (c) 2019 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.scp;

/** SCP Command codes for running core control. */
enum RunningCommand implements CommandCode {
	/** Ask the core to stop. */
	STOP_ID(6),
	/** Set the running time and mode. */
	NEW_RUNTIME_ID(7),
	/** Update the provenance and exit. */
	UPDATE_PROVENCE_REGION_AND_EXIT(8),
	/** Clear the IOBUF of the core. */
	CLEAR_IOBUF(9);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	RunningCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
