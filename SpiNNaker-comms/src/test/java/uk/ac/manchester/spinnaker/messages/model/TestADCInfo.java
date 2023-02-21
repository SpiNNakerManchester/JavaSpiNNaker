/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.model;

import static java.nio.ByteBuffer.wrap;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.manchester.spinnaker.messages.model.SerSupport.deserialize;
import static uk.ac.manchester.spinnaker.messages.model.SerSupport.serialize;

import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

@SuppressWarnings("ClassCanBeStatic")
class TestADCInfo {
	private static final int SIZE = 48;

	@Test
	void zeroSer() throws IOException, ClassNotFoundException {
		var bytes = new byte[SIZE];
		Arrays.fill(bytes, (byte) 23); // Anything non-zero
		var adcIn = new ADCInfo(wrap(bytes).order(LITTLE_ENDIAN));
		var serialForm = serialize(adcIn);

		var adcOut = deserialize(serialForm, ADCInfo.class);

		// No overall equality op; by field comparison
		assertEquals(adcIn.fan0, adcOut.fan0);
		assertEquals(adcIn.fan1, adcOut.fan1);
		assertEquals(adcIn.tempBottom, adcOut.tempBottom);
		assertEquals(adcIn.tempTop, adcOut.tempTop);
		assertEquals(adcIn.tempExt0, adcOut.tempExt0);
		assertEquals(adcIn.tempExt1, adcOut.tempExt1);
		assertEquals(adcIn.voltage12a, adcOut.voltage12a);
		assertEquals(adcIn.voltage12b, adcOut.voltage12b);
		assertEquals(adcIn.voltage12c, adcOut.voltage12c);
		assertEquals(adcIn.voltage18, adcOut.voltage18);
		assertEquals(adcIn.voltage33, adcOut.voltage33);
		assertEquals(adcIn.voltageSupply, adcOut.voltageSupply);
	}
}
