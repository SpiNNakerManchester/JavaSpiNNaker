/*
 * Copyright (c) 2018 The University of Manchester
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
	public static final int MS_PER_S = 1000;

    /** The number of millisecond per minute. */
	public static final double MS_PER_M = 60000.0;

   /** The number of second per minute. */
	public static final int S_PER_M = 60;

    /** The number of millisecond per hour. */
	public static final double MS_PER_H = 3600000.0;

    /** The number of millisecond per hour. */
	public static final int M_PER_H = 60;

    /**
     * Formats a Duration with hours, minutes seconds and milliseconds
     *      as required.
     *
     * @param durationInMillis A time interval in milliseconds
     *
     * @return A formated String with only the relative units.
     */
    public static String formatDuration(long durationInMillis) {
        long hr = TimeUnit.MILLISECONDS.toHours(durationInMillis);
        long min = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) % M_PER_H;
        long sec = TimeUnit.MILLISECONDS.toSeconds(durationInMillis) % S_PER_M;
        long ms = TimeUnit.MILLISECONDS.toMillis(durationInMillis) % MS_PER_S;
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
