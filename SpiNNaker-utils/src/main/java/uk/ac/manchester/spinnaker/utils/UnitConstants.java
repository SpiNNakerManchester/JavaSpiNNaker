/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 *
 * @author Christian-B
 */
public final class UnitConstants {

    private UnitConstants() { }

    /** The number of megahertz in each kilohertz. */
    public static final int MEGAHERTZ_PRE_KILOHERTZ = 1000;

    /** The number of megahertz in each hertz. */
    public static final int MEGAHERTZ_PRE_HERTZ = 1000000;

    /** The number of milliseconds per second. */
	public static final double MS_PER_S = 1000.0;

    /** The number of second per minute. */
	public static final double MS_PER_M = 60000.0;

    /** The number of second per hour. */
	public static final double MS_PER_H = 3600000.0;

    /**
     * Formats a Duration with hours, minutes seconds and milliseconds
     *      as required.
     *
     * @param durationInMillis A time interval in milliseconds
     *
     * @return A formated String with only the relative units.
     */
    public static String formatDuration(long durationInMillis) {
        if (durationInMillis < MS_PER_S) {
            return durationInMillis + " ms";
        }
        if (durationInMillis < MS_PER_M) {
            DateFormat formatter = new SimpleDateFormat("s.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = new Date(durationInMillis);
            return formatter.format(date) + " s";
        }
        if (durationInMillis < MS_PER_H) {
            DateFormat formatter = new SimpleDateFormat("m:ss.SSS");
            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = new Date(durationInMillis);
            return formatter.format(date) + " m";
        }
        DateFormat formatter = new SimpleDateFormat("H:mm:ss.SSS");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date(durationInMillis);
        return formatter.format(date) + " h";
    }
}
