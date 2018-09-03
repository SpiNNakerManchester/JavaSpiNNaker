/*
 * Copyright (c) 2018 The University of Manchester
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
