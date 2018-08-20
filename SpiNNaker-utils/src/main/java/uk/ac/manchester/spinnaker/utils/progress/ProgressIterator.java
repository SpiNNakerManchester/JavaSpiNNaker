/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 *
 * @author Christian-B
 */
public class ProgressIterator<E>  implements Iterator<E>, Closeable{

    private final ProgressBar bar; // = new ProgressBar(things.size(), description);

    private final Iterator<E> inner; // = things.iterator();

    public ProgressIterator(
            Collection<E> outer, String description, PrintStream output) {
        bar = new ProgressBar(outer.size(), description, output);
        inner = outer.iterator();
    }

    @Override
    public boolean hasNext() {
        boolean result = inner.hasNext();
        if (!result) {
            bar.close();
        }
        return result;
    }

    @Override
    public E next() {
        bar.update();
        return inner.next();
    }

    @Override
    public void close() {
        bar.close();
    }

    public void setOutput(PrintStream output) {
        bar.setOutput(output);
    }

    public void resetOutput() {
        bar.resetOutput();
    }
}
