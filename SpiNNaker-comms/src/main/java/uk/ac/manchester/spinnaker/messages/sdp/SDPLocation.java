/*
 * Copyright (c) 2023 The University of Manchester
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
package uk.ac.manchester.spinnaker.messages.sdp;

import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.board.BMPLocation;
import uk.ac.manchester.spinnaker.machine.board.HasBMPLocation;

/**
 * Represents a location in an SDP message.  This could be a core location
 * or a BMP location.  As such it can be interpreted as either.
 */
public class SDPLocation implements HasCoreLocation, HasBMPLocation {

	/** Shift for major in hash code. **/
	private static final int MAJOR_SHIFT = 24;

	/** Shift for minor in hash code. **/
	private static final int MINOR_SHIFT = 16;

	/** The first value. **/
	final int major;

	/** The second value. **/
	final int minor;

	/** The third value. **/
	final int detail;

	/** Was constructed from a core location. **/
	private final boolean fromCoreLocation;

	/** Was constructed from a BMP location. **/
	private final boolean fromBMPLocation;

	/**
	 * Make an SDPLocation from a core location.
	 *
	 * @param core The core location to use.
	 */
	public SDPLocation(HasCoreLocation core) {
		major = core.getX();
		minor = core.getY();
		detail = core.getP();
		fromCoreLocation = true;
		fromBMPLocation = false;
	}

	/**
	 * Make an SDPLocation from a BMP location.
	 *
	 * @param bmp The BMP location to use.
	 */
	public SDPLocation(HasBMPLocation bmp) {
		major = bmp.getCabinet();
		minor = bmp.getFrame();
		detail = bmp.getBoard();
		fromCoreLocation = false;
		fromBMPLocation = true;
	}

	/**
	 * Make an SDPLocation from components.
	 *
	 * @param major The first part of the location (x or cabinet).
	 * @param minor The second part of the location (y or frame).
	 * @param detail The third part of the location (p or board).
	 */
	public SDPLocation(int major, int minor, int detail) {
		this.major = major;
		this.minor = minor;
		this.detail = detail;
		fromCoreLocation = false;
		fromBMPLocation = false;
	}

	private void errorIfBMP() {
		if (fromBMPLocation) {
			throw new InvalidSDPHeaderUseException("This SDP Location is"
					+ " specifically constructed from a BMPLocation so use with"
					+ " CoreLocation properties is not supported!");
		}
	}

	private void errorIfCore() {
		if (fromCoreLocation) {
			throw new InvalidSDPHeaderUseException("This SDP Location is"
					+ " specifically constructed from a CoreLocation so use"
					+ " with BMPLocation properties is not supported!");
		}
	}

	@Override
	public int getX() {
		errorIfBMP();
		return major;
	}

	@Override
	public int getY() {
		errorIfBMP();
		return minor;
	}

	@Override
	public int getP() {
		errorIfBMP();
		return detail;
	}

	@Override
	public int getCabinet() {
		errorIfCore();
		return major;
	}

	@Override
	public int getFrame() {
		errorIfCore();
		return minor;
	}

	@Override
	public int getBoard() {
		errorIfCore();
		return detail;
	}

	@Override
	public ChipLocation asChipLocation() {
		errorIfBMP();
		return HasCoreLocation.super.asChipLocation();
	}

	@Override
	public CoreLocation asCoreLocation() {
		errorIfBMP();
		return HasCoreLocation.super.asCoreLocation();
	}

	@Override
	public BMPLocation asBMPLocation() {
		errorIfCore();
		return HasBMPLocation.super.asBMPLocation();
	}

	@Override
	public String toString() {
		if (fromCoreLocation) {
			return "SDPLocation(X=" + major + ", Y=" + minor
					+ ", P=" + detail + ")";
		}
		if (fromBMPLocation) {
			return "SDPLocation(Cabinet=" + major + ", Frame=" + minor
					+ ", Board=" + detail + ")";
		}
		return "SDPLocation(" + major + ", " + minor + ", " + detail + ")";
	}

	@Override
	public int hashCode() {
		return (major << MAJOR_SHIFT) + (minor << MINOR_SHIFT) + detail;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SDPLocation)) {
			return false;
		}
		SDPLocation sdp = (SDPLocation) obj;
		return sdp.major == major && sdp.minor == minor && sdp.detail == detail;
	}

	private static class InvalidSDPHeaderUseException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InvalidSDPHeaderUseException(String reason) {
			super(reason);
		}
	}
}
