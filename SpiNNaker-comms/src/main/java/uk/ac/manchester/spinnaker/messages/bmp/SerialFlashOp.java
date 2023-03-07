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
package uk.ac.manchester.spinnaker.messages.bmp;

import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The serial flash operations for argument three of
 * {@link SCPCommand#CMD_BMP_SF}.
 */
@UsedInJavadocOnly(SCPCommand.class)
enum SerialFlashOp {
	/** Read from the Serial Flash. */
	READ(0),
	/** write to the Serial Flash. */
	WRITE(1),
	/** Get the CRC of Serial Flash. */
	CRC(2);

	/** The raw BMP value. */
	public final byte value;

	SerialFlashOp(int value) {
		this.value = (byte) value;
	}
}
