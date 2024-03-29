/*
 * Copyright (c) 2018 The University of Manchester
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

/** SCP IP tag Commands. For {@code cmd_iptag()} in {@code scamp-cmd.c}. */
enum IPTagCommand {
	/** Create. */
	NEW(0),
	/** Update. */
	SET(1),
	/** Fetch. */
	GET(2),
	/** Delete. */
	CLR(3),
	/** Update Meta. */
	TTO(4);

	/** The SCAMP-encoded value. */
	public final byte value;

	IPTagCommand(int value) {
		this.value = (byte) value;
	}
}
