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
package uk.ac.manchester.spinnaker.machine;

import java.util.ArrayList;

/**
 * Tuple class to return a CoreSubsets and a List of Failed Chips.
 * <p>
 * This class acts like a CoreSubsets with the add on of a list of Chips.
 * <p>
 * Note: That to allow this to be used exactly has a CoreSubsets,
 *      methods such as hashcode and equals intentionally ignore the
 *      failedChips object.
 *      IE These have not been overwritten.
 *
 * @author Christian-B
 */
public class CoreSubsetsFailedChipsTuple extends CoreSubsets {

    /** List of the Chips that failed. */
    public final ArrayList<Chip> failedChips;

    /**
     * Basic Constructor which sets up all the holding objects.
     */
    CoreSubsetsFailedChipsTuple() {
        super();
        failedChips = new ArrayList<>();
    }

    /**
     * Adds a failed chip to the list of failed chips.
     *
     * @param chip Chip to add as failed.
     */
    public void addFailedChip(Chip chip) {
        failedChips.add(chip);
    }

}
