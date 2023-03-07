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
package uk.ac.manchester.spinnaker.messages.model;

import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_FAN;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_TEMP;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TEMP_SCALE;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_12;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_2_5;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_3_3;

import java.nio.ByteBuffer;

/** Container for the ADC data thats been retrieved from an FPGA. */
@SARKStruct(value = "board_stat_t", api = SARKStruct.API.BMP)
public final class ADCInfo {
	/** fan<sub>0</sub> rotation rate. */
	@SARKField("fan_0")
	public final Double fan0;

	/** fan<sub>1</sub> rotation rate. */
	@SARKField("fan_1")
	public final Double fan1;

	/** temperature bottom. */
	@SARKField("temp_btm")
	public final double tempBottom;

	/** temperature external<sub>0</sub>. */
	@SARKField("temp_ext_0")
	public final Double tempExt0;

	/** temperature external<sub>1</sub>. */
	@SARKField("temp_ext_1")
	public final Double tempExt1;

	/** temperature top. */
	@SARKField("temp_top")
	public final double tempTop;

	/** Actual voltage of the 1.2V<sub>a</sub> supply rail. */
	@SARKField("voltage_1_2a")
	public final double voltage12a;

	/** Actual voltage of the 1.2V<sub>b</sub> supply rail. */
	@SARKField("voltage_1_2b")
	public final double voltage12b;

	/** Actual voltage of the 1.2V<sub>c</sub> supply rail. */
	@SARKField("voltage_1_2c")
	public final double voltage12c;

	/** Actual voltage of the 1.8V supply rail. */
	@SARKField("voltage_1_8")
	public final double voltage18;

	/** Actual voltage of the 3.3V supply rail. */
	@SARKField("voltage_3_3")
	public final double voltage33;

	/** Actual voltage of the main power supply (nominally 12V?). */
	@SARKField("voltage_supply")
	public final double voltageSupply;

	// Sizes of arrays
	private static final int ADC_SIZE = 8;

	private static final int T_INT_SIZE = 4;

	private static final int T_EXT_SIZE = 4;

	private static final int FAN_SIZE = 4;

	// Indices into arrays
	private static final int V_1_2C = 1;

	private static final int V_1_2B = 2;

	private static final int V_1_2A = 3;

	private static final int V_1_8 = 4;

	private static final int V_3_3 = 6;

	private static final int VS = 7;

	private static final int T_TOP = 0;

	private static final int T_BTM = 1;

	private static final int T_X0 = 0;

	private static final int T_X1 = 1;

	private static final int FAN0 = 0;

	private static final int FAN1 = 1;

	/**
	 * @param buffer
	 *            bytes from an SCP packet containing ADC information
	 */
	public ADCInfo(ByteBuffer buffer) {
		var adc = new short[ADC_SIZE];
		var tInt = new short[T_INT_SIZE];
		var tExt = new short[T_EXT_SIZE];
		var fan = new short[FAN_SIZE];
		var sb = buffer.asShortBuffer().asReadOnlyBuffer();
		sb.get(adc);
		sb.get(tInt);
		sb.get(tExt);
		sb.get(fan);

		voltage12c = adc[V_1_2C] * BMP_V_SCALE_2_5;
		voltage12b = adc[V_1_2B] * BMP_V_SCALE_2_5;
		voltage12a = adc[V_1_2A] * BMP_V_SCALE_2_5;
		voltage18 = adc[V_1_8] * BMP_V_SCALE_2_5;
		voltage33 = adc[V_3_3] * BMP_V_SCALE_3_3;
		voltageSupply = adc[VS] * BMP_V_SCALE_12;
		tempTop = tempScale(tInt[T_TOP]);
		tempBottom = tempScale(tInt[T_BTM]);
		tempExt0 = tempScale(tExt[T_X0]);
		tempExt1 = tempScale(tExt[T_X1]);
		fan0 = fanScale(fan[FAN0]);
		fan1 = fanScale(fan[FAN1]);
	}

	private static Double tempScale(int rawValue) {
		if (rawValue == BMP_MISSING_TEMP) {
			return null;
		}
		return rawValue * BMP_TEMP_SCALE;
	}

	private static Double fanScale(int rawValue) {
		if (rawValue == BMP_MISSING_FAN) {
			return null;
		}
		return (double) rawValue;
	}
}
