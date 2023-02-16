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
package uk.ac.manchester.spinnaker.messages.bmp;

import uk.ac.manchester.spinnaker.messages.scp.SCPCommand;
import uk.ac.manchester.spinnaker.utils.UsedInJavadocOnly;

/**
 * The SCP BMP Information types. Used as the first argument of
 * {@link SCPCommand#CMD_BMP_INFO}.
 */
@UsedInJavadocOnly(SCPCommand.class)
enum BMPInfo {
	/** Serial flash information. */
	SERIAL(0),
	/** Data read from EEPROM. {@code ee_data_t} contents. */
	EE_BUF(1),
	/** CAN status information. */
	CAN_STATUS(2),
	/** ADC information. */
	ADC(3),
	/** IP Address. */
	IP_ADDR(4),
	/** Uninitialised vector. 8 words, from {@code uni_vec}. */
	UNINIT_VEC(5);

	/** The raw BMP value. */
	public final byte value;

	BMPInfo(int value) {
		this.value = (byte) value;
	}
}
