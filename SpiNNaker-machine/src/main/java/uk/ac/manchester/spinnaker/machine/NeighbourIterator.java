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

import java.util.Iterator;

/**
 *
 * @author Christian-B
 */
public class NeighbourIterator implements Iterator<ChipLocation> {

    private Iterator<Link> linksIter;

    NeighbourIterator(Iterator<Link> linksIter) {
        this.linksIter = linksIter;
    }

    @Override
    public boolean hasNext() {
        return linksIter.hasNext();
    }

    @Override
    public ChipLocation next() {
        return linksIter.next().destination;
    }

}
