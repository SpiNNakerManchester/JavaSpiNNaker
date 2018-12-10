/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Christian-B
 * @author Donal Fellows
 */
public final class UnitConstants {

    private UnitConstants() { }

    /** The number of megahertz in each kilohertz. */
    public static final int MEGAHERTZ_PER_KILOHERTZ = 1000;

    /** The number of megahertz in each hertz. */
    public static final int MEGAHERTZ_PER_HERTZ = 1000000;

    /** The number of milliseconds per second. */
	public static final int MSEC_PER_SEC = 1000;

   /** The number of second per minute. */
	public static final int SEC_PER_MINUTE = 60;

    /** The number of minute per hour. */
	public static final int MINUTE_PER_HOUR = 60;

    /**
     * Formats a Duration with hours, minutes seconds and milliseconds
     *      as required.
     *
     * @param durationInMillis A time interval in milliseconds
     *
     * @return A formatted String with only the relative units.
     */
    public static String formatDuration(long durationInMillis) {
        long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis)
                % MINUTE_PER_HOUR;
        long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis)
                % SEC_PER_MINUTE;
        long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis)
                % MSEC_PER_SEC;
        if (hr > 0) {
            return String.format("%d:%02d:%02d.%03d h", hr, min, sec, ms);
        }
        if (min > 0) {
            return String.format("%d:%02d.%03d m", min, sec, ms);

        }
        if (sec > 0) {
            return String.format("%d.%03d s", sec, ms);

        }
        return durationInMillis + " ms";
    }
}
