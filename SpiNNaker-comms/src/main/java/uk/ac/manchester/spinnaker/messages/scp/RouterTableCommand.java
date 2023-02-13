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
package uk.ac.manchester.spinnaker.messages.scp;

/** SDP Command codes to the extra monitors for router table control. */
enum RouterTableCommand implements CommandCode {
	/**
	 * Save the application multicast routes, which must be currently installed.
	 */
	SAVE_APPLICATION_ROUTES(6),
	/** Load the (previously saved) application multicast routes. */
	LOAD_APPLICATION_ROUTES(7),
	/** Load the (previously configured) system multicast routes. */
	LOAD_SYSTEM_ROUTES(8);

	/**
	 * The encoded form of the command.
	 */
	private final short value;

	RouterTableCommand(int cmd) {
		this.value = (short) cmd;
	}

	@Override
	public short getValue() {
		return value;
	}
}
