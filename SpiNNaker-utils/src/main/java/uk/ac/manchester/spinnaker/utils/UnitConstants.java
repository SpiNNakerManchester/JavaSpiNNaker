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
package uk.ac.manchester.spinnaker.utils;

import static java.lang.String.format;

import java.time.Duration;

/**
 * Constants relating to physical units.
 *
 * @author Christian-B
 * @author Donal Fellows
 */
public final class UnitConstants {
	private UnitConstants() {
	}

	/** The number of megahertz in each kilohertz. */
	public static final int MEGAHERTZ_PER_KILOHERTZ = 1000;

	/** The number of megahertz in each hertz. */
	public static final int MEGAHERTZ_PER_HERTZ = 1000000;

	/** The number of milliseconds per second. */
	public static final int MSEC_PER_SEC = 1000;

	/** The number of nanoseconds per second. */
	public static final int NSEC_PER_SEC = 1000000000;

	/** The number of second per minute. */
	public static final int SEC_PER_MINUTE = 60;

	/** The number of minute per hour. */
	public static final int MINUTE_PER_HOUR = 60;

	/** The number of nanoseconds per microsecond. */
	public static final double NSEC_PER_USEC = 1000.0;

	/** The number of nanoseconds per millisecond. */
	public static final double NSEC_PER_MSEC = 1000000.0;

	/**
	 * Formats a duration with hours, minutes seconds and milliseconds as
	 * required.
	 *
	 * @param durationInMillis
	 *            A time interval in milliseconds
	 * @return A formatted String with only the relative units.
	 */
	public static String formatDuration(long durationInMillis) {
		var d = Duration.ofMillis(durationInMillis);
		long hr = d.toHours();
		long min = d.toMinutesPart();
		long sec = d.toSecondsPart();
		long ms = d.toMillisPart();
		if (hr > 0) {
			return format("%d:%02d:%02d.%03d h", hr, min, sec, ms);
		}
		if (min > 0) {
			return format("%d:%02d.%03d m", min, sec, ms);
		}
		if (sec > 0) {
			return format("%d.%03d s", sec, ms);
		}
		return ms + " ms";
	}
}
