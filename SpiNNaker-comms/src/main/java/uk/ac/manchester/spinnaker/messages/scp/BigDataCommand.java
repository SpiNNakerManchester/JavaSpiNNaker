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
package uk.ac.manchester.spinnaker.messages.scp;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

/** Commands supported by Big Data */
public enum BigDataCommand implements CommandCode {

    /**
     * Initialise Big Data.
     */
    BIG_DATA_INIT(0),

    /**
     * Free Big Data.
     */
    BIG_DATA_FREE(1),

    /**
     * Get Big Data information.
     */
    BIG_DATA_INFO(2);


    /** The SCAMP encoding. */
    public final short value;
    private static final Map<Short, BigDataCommand> MAP = new HashMap<>();

    BigDataCommand(int value) {
        this.value = (short) value;
    }

    static {
        for (BigDataCommand r : values()) {
            MAP.put(r.value, r);
        }
    }

    /**
    * Convert an encoded value into an enum element.
    *
    * @param value
    *            The value to convert
    * @return The enum element
    */
    public static BigDataCommand get(short value) {
        return requireNonNull(MAP.get(value),
                "unrecognised command value: " + value);
    }

    @Override
    public short getValue() {
        return value;
    }

}
