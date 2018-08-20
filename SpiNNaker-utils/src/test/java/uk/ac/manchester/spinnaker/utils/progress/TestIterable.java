/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import uk.ac.manchester.spinnaker.utils.Counter;

/**
 *
 * @author Christian-B
 */
public class TestIterable {

    private static final String PERCENTS =
            "|0%                          50%                         100%|";
    private static final String DASHES =
            " ------------------------------------------------------------";


    public TestIterable() {
    }

    @Test
    public void testSimple() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Easiest";
        ProgressIterable<Integer> pb = new ProgressIterable<>(
                Arrays.asList(1,2,3,4,5), description);
        pb.setOutput(ps);
        int sum = 0;
        for (int i:pb){
            sum += i;
        }
        assertEquals(1+2+3+4+5, sum);
        String lines[] = baos.toString().split("\\r?\\n");
        assertEquals(3, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(DASHES, lines[2]);
    }

    @Test
    public void testStopEarly() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Early";
        try (ProgressIterable<Integer> bar = new ProgressIterable<Integer>(
                Arrays.asList(1,2,3,4,5,6,7,8,9,10), description); ) {
            bar.setOutput(ps);
            for (int i:bar) {
                if (i == 3) {
                    break;
                }
            }
        }
        String lines[] = baos.toString().split("\\r?\\n");
        assertEquals(3, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(60 / 10 * 3 + 1, lines[2].length());
    }

    @Test
    public void testWeirdEarly() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Early";
        try (ProgressIterable<Integer> bar = new ProgressIterable<Integer>(
                Arrays.asList(1,2,3,4,5,6,7,8,9,10), description); ) {
            Iterator<Integer> iterator = bar.iterator();
            bar.setOutput(ps);
            assertEquals(1, (int)iterator.next());
            assertEquals(2, (int)iterator.next());
            bar.resetOutput();
            assertEquals(3, (int)iterator.next());
            bar.setOutput(ps);
            assertEquals(4, (int)iterator.next());
            bar.setOutput(null);
            assertEquals(5, (int)iterator.next());
        }
        String lines[] = baos.toString().split("\\r?\\n");
        // Header lines not there as created on Iterator call
        assertEquals(1, lines.length);
        // First space written before setOutput
        // Two to stream and then one more second time around.
        assertEquals(60 / 10 * 3, lines[0].length());
    }

    @Test
    public void testForEachRemaining() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Easiest";
        ProgressIterable<Integer> pb = new ProgressIterable<>(
                Arrays.asList(1,2,3,4,5), description);
        pb.setOutput(ps);
        Counter sum = new Counter();
        pb.forEach(i -> {;
            sum.add(i);
        });
        assertEquals(1+2+3+4+5, sum.get());
        String lines[] = baos.toString().split("\\r?\\n");
        assertEquals(3, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(DASHES, lines[2]);
    }

}


