package uk.ac.manchester.spinnaker.messages.model;

import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_FAN;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_MISSING_TEMP;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_TEMP_SCALE;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_12;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_2_5;
import static uk.ac.manchester.spinnaker.messages.Constants.BMP_V_SCALE_3_3;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/** Container for the ADC data thats been retrieved from an FPGA. */
public final class ADCInfo {
	/** fan 0 rotation rate */
	public final Double fan_0;
	/** fan 1 rotation rate */
	public final Double fan_1;
	/** temperature bottom */
	public final double temp_btm;
	/** temperature external 0 */
	public final Double temp_ext_0;
	/** temperature external 1 */
	public final Double temp_ext_1;
	/** temperature top */
	public final double temp_top;
	/** Actual voltage of the 1.2V a supply rail */
	public final double voltage_1_2a;
	/** Actual voltage of the 1.2V b supply rail */
	public final double voltage_1_2b;
	/** Actual voltage of the 1.2V c supply rail */
	public final double voltage_1_2c;
	/** Actual voltage of the 1.8V supply rail */
	public final double voltage_1_8;
	/** Actual voltage of the 3.3V supply rail */
	public final double voltage_3_3;
	/** Actual voltage of the main power supply (nominally 12V?). */
	public final double voltage_supply;

	/**
	 * @param buffer
	 *            bytes from an SCP packet containing ADC information
	 */
	public ADCInfo(ByteBuffer buffer) {
		short[] adc = new short[8];
		short[] t_int = new short[4];
		short[] t_ext = new short[4];
		short[] fan = new short[4];
		ShortBuffer sb = buffer.asShortBuffer().asReadOnlyBuffer();
		sb.get(adc);
		sb.get(t_int);
		sb.get(t_ext);
		sb.get(fan);

		voltage_1_2c = adc[1] * BMP_V_SCALE_2_5;
		voltage_1_2b = adc[2] * BMP_V_SCALE_2_5;
		voltage_1_2a = adc[3] * BMP_V_SCALE_2_5;
		voltage_1_8 = adc[4] * BMP_V_SCALE_2_5;
		voltage_3_3 = adc[6] * BMP_V_SCALE_3_3;
		voltage_supply = adc[7] * BMP_V_SCALE_12;
		temp_top = t_scale(t_int[0]);
		temp_btm = t_scale(t_int[1]);
		temp_ext_0 = t_scale(t_ext[0]);
		temp_ext_1 = t_scale(t_ext[1]);
		fan_0 = f_scale(fan[0]);
		fan_1 = f_scale(fan[1]);
	}

	private static Double t_scale(int rawValue) {
		if (rawValue == BMP_MISSING_TEMP) {
			return null;
		}
		return rawValue * BMP_TEMP_SCALE;
	}

	private static Double f_scale(int rawValue) {
		if (rawValue == BMP_MISSING_FAN) {
			return null;
		}
		return (double) rawValue;
	}
}