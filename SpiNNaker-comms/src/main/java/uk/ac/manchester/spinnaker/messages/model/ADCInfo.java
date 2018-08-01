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
@SuppressWarnings("checkstyle:MemberName")
// Member names match field names in BMP; do not change!
public final class ADCInfo {
	/** fan<sub>0</sub> rotation rate. */
	public final Double fan_0;
	/** fan<sub>1</sub> rotation rate. */
	public final Double fan_1;
	/** temperature bottom. */
	public final double temp_btm;
	/** temperature external<sub>0</sub>. */
	public final Double temp_ext_0;
	/** temperature external<sub>1</sub>. */
	public final Double temp_ext_1;
	/** temperature top. */
	public final double temp_top;
	/** Actual voltage of the 1.2V<sub>a</sub> supply rail. */
	public final double voltage_1_2a;
	/** Actual voltage of the 1.2V<sub>b</sub> supply rail. */
	public final double voltage_1_2b;
	/** Actual voltage of the 1.2V<sub>c</sub> supply rail. */
	public final double voltage_1_2c;
	/** Actual voltage of the 1.8V supply rail. */
	public final double voltage_1_8;
	/** Actual voltage of the 3.3V supply rail. */
	public final double voltage_3_3;
	/** Actual voltage of the main power supply (nominally 12V?). */
	public final double voltage_supply;

	private static final int ADC_SIZE = 8;
	private static final int T_INT_SIZE = 4;
	private static final int T_EXT_SIZE = 4;
	private static final int FAN_SIZE = 4;

	/**
	 * @param buffer
	 *            bytes from an SCP packet containing ADC information
	 */
	@SuppressWarnings("checkstyle:LocalVariableName")
	public ADCInfo(ByteBuffer buffer) {
		short[] adc = new short[ADC_SIZE];
		short[] t_int = new short[T_INT_SIZE];
		short[] t_ext = new short[T_EXT_SIZE];
		short[] fan = new short[FAN_SIZE];
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
		temp_top = tempScale(t_int[0]);
		temp_btm = tempScale(t_int[1]);
		temp_ext_0 = tempScale(t_ext[0]);
		temp_ext_1 = tempScale(t_ext[1]);
		fan_0 = fanScale(fan[0]);
		fan_1 = fanScale(fan[1]);
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
