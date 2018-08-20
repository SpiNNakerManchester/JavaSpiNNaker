/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Christian-B
 * @param <E>
 */
public class ProgressIterable<E> implements Iterable<E>, Closeable  {

    private final Collection<E> things;
    private final String description;
    private final ArrayList<ProgressIterator<E>> progressIterables;
    private PrintStream output = System.out;

    public ProgressIterable(Collection<E> things, String description) {
        this.things = things;
        this.description = description;
        progressIterables = new ArrayList<>();
    }

    @Override
    public Iterator<E> iterator() {
        ProgressIterator<E> iterator =
                new ProgressIterator(things, description, output);
        progressIterables.add(iterator);
        return iterator;
    }

    @Override
    public void close() {
        Iterator<ProgressIterator<E>> iter = progressIterables.iterator();
        while(iter.hasNext()) {
            iter.next().close();
            iter.remove();
        }
    }

    public void setOutput(PrintStream output) {
        this.output = output;
        for (ProgressIterator<E> iter: progressIterables) {
            iter.setOutput(output);
        }
    }

    public void resetOutput() {
        this.output = System.out;
        for (ProgressIterator<E> iter: progressIterables) {
            iter.resetOutput();
        }
    }

}
