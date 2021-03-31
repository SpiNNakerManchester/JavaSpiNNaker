/*
 * Copyright (c) 2021 The University of Manchester
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
package uk.ac.manchester.spinnaker.data_spec;

/**
 * Marker of something that is a memory region.
 */
public interface MemoryRegion {

    /** @return the index of the memory region. */
    int getIndex();

    /** @return the base address of the region. */
    int getRegionBase();

    /**
     * Set the base address of the region.
     * @param baseAddress The base address to set.
     */
    void setRegionBase(int baseAddress);

}
