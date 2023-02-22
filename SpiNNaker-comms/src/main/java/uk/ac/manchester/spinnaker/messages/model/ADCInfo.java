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
package uk.ac.manchester.spinnaker.messages.model;

import static java.lang.Short.toUnsignedInt;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_FAN;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_TEMP;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_12;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_2_5;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_3_3;

import java.io.Serializable;
import java.nio.ByteBuffer;

/** Container for the board status data thats been retrieved from a BMP. */
@SARKStruct(value = "board_stat_t", api = SARKStruct.API.BMP)
public final class ADCInfo implements Serializable {
	private static final long serialVersionUID = 8245655123474028462L;

	/**
	 * The number of bytes the board status structure is encoded as in the BMP.
	 */
	public static final int SIZE = 48;

	/** fan<sub>0</sub> rotation rate. */
	@SARKField("fan_0")
	public final Double fan0;

	/** fan<sub>1</sub> rotation rate. */
	@SARKField("fan_1")
	public final Double fan1;

	/** fan<sub>2</sub> rotation rate. */
	@SARKField("fan_2")
	public final Double fan2;

	/** Temperature bottom, in &deg;C. */
	@SARKField("temp_btm")
	public final double tempBottom;

	/**
	 * Temperature external<sub>0</sub>, in &deg;C. Only meaningful when data
	 * was obtained from a BMP that manages a frame.
	 */
	@SARKField("temp_ext_0")
	public final Double tempExt0;

	/**
	 * Temperature external<sub>1</sub>, in &deg;C. Only meaningful when data
	 * was obtained from a BMP that manages a frame.
	 */
	@SARKField("temp_ext_1")
	public final Double tempExt1;

	/** Temperature top, in &deg;C. */
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

	/** Warning flags. See {@code check_status()} in {@code bmp_main.c}. */
	@SARKField("warning")
	public final int warningFlags;

	/**
	 * Shutdown flags. See {@code check_status()} in {@code bmp_main.c}. If
	 * non-zero, the board is very unhappy with its status, and will be flashing
	 * an LED rapidly.
	 */
	@SARKField("shutdown")
	public final int shutdownFlags;

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

	private static final int FAN2 = 2;

	private static final int WARNING_OFFSET = 40;

	private static final int SHUTDOWN_OFFSET = 44;

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
		warningFlags = buffer.get(WARNING_OFFSET);
		shutdownFlags = buffer.get(SHUTDOWN_OFFSET);

		voltage12c = toUnsignedInt(adc[V_1_2C]) * BMP_V_SCALE_2_5;
		voltage12b = toUnsignedInt(adc[V_1_2B]) * BMP_V_SCALE_2_5;
		voltage12a = toUnsignedInt(adc[V_1_2A]) * BMP_V_SCALE_2_5;
		voltage18 = toUnsignedInt(adc[V_1_8]) * BMP_V_SCALE_2_5;
		voltage33 = toUnsignedInt(adc[V_3_3]) * BMP_V_SCALE_3_3;
		voltageSupply = toUnsignedInt(adc[VS]) * BMP_V_SCALE_12;
		tempTop = tempScale(tInt[T_TOP]);
		tempBottom = tempScale(tInt[T_BTM]);
		tempExt0 = tempScale(tExt[T_X0]);
		tempExt1 = tempScale(tExt[T_X1]);
		fan0 = fanScale(fan[FAN0]);
		fan1 = fanScale(fan[FAN1]);
		fan2 = fanScale(fan[FAN2]);
	}

	// Bottom 5 bits are meaningless
	private static final int TEMP_MASK = 0b00011111;

	// Upper byte is whole number of degrees C
	private static final double TEMP_FACTOR = 256;

	private static Double tempScale(int rawValue) {
		if (rawValue == BMP_MISSING_TEMP) {
			return null;
		}
		// See https://www.nxp.com/docs/en/data-sheet/LM75B.pdf Sec 7.4.3
		int masked = rawValue & ~TEMP_MASK;
		return masked / TEMP_FACTOR;
	}

	private static Double fanScale(short rawValue) {
		if (rawValue == BMP_MISSING_FAN) {
			return null;
		}
		return (double) toUnsignedInt(rawValue);
	}
}
