/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.Closeable;
import java.io.PrintStream;
import java.util.Iterator;

/**
 *
 * @author Christian-B
 */
public abstract class AbstractProgress implements Closeable {

    private static final int MAX_LENGTH_IN_CHARS = 60;
    private static final float MAX_LENGTH = MAX_LENGTH_IN_CHARS;
    private static final String DISTANCE_INDICATOR = distanceIndicator();
    private static PrintStream output = System.out;

    private final char stepChar = '-';
    private final char endChar = '|';
    private final char startChar = '|';

    private final int numberOfThings;
    private final float charsPerThing;
    private int currentlyCompleted = 0;
    private int charsDone = 0;
    private boolean closed = false;

    AbstractProgress(int numberOfThings, String description) {
        this.numberOfThings = numberOfThings;
        charsPerThing = MAX_LENGTH / numberOfThings;
        printHeader(description);
    }

    private void printHeader(String description) {
        if (description != null) {
            output.println(description);
        }
        output.println(startChar + DISTANCE_INDICATOR + endChar);
        output.print(" ");
    }

    void calcUpdate(int amountToAdd) {
        if ((currentlyCompleted + amountToAdd) > numberOfThings) {
            throw new IllegalStateException("too many update steps");
        }
        currentlyCompleted += amountToAdd;
        if (currentlyCompleted == numberOfThings) {
           close();
        } else {
            printProgress((int)(currentlyCompleted * charsPerThing));
        }
    }

    private void printProgress(int expectedCharsDone) {
        for (int i = charsDone; i < expectedCharsDone; i++) {
            output.print(stepChar);
        }
        charsDone = expectedCharsDone;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        output.println();
        closed = true;
    }

    private static String distanceIndicator() {
        // Find the mid point
        int mid_point = AbstractProgress.MAX_LENGTH_IN_CHARS / 2;

        StringBuilder builder = new StringBuilder("0%");

        // The space between 0% and 50% is the mid-point minus the width of
        // 0% and ~half the width of 50%
         for (int i= 0; i < mid_point - 4; i += 1) {
             builder.append(" ");
        }

        builder.append("50%");

        // The space between 50% and 100% is the mid-point minus the rest of
        // the width of 50% and the width of 100%
        for (int i = 0; i < mid_point - 5; i += 1) {
             builder.append(" ");
        }

        builder.append("100%");

        return builder.toString();
    }

}
