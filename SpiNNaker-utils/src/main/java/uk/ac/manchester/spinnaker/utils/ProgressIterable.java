/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Christian-B
 * @param <E>
 */
public class ProgressIterable<E> extends ProgressBar implements Iterable<E>{

    private final Collection<E> things;
    private boolean started = false;

    public ProgressIterable(Collection<E> things, String description) {
        super(things.size(), description);
        this.things = things;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> inner = things.iterator();

            @Override
            public boolean hasNext() {
                return inner.hasNext();
            }

            @Override
            public E next() {
                return inner.next();
            }
        };
    }

    public static void main(String [] args) throws InterruptedException {
        ProgressBar pb = new ProgressBar(5, "Easiest");
        for (int i = 0; i < 5 ; i++){
            pb.update();
        }
        System.out.println("Simple");
        for (Integer i:  new ProgressIterable<Integer>(Arrays.asList(1,2,3,4,5), "iterable")){

        }
    }

}
