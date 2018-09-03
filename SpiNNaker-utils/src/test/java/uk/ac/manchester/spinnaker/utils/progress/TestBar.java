/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils.progress;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestBar {

    private static final String PERCENTS =
            "|0%                          50%                         100%|";
    private static final String DASHES =
            " ------------------------------------------------------------";


    public TestBar() {
    }

    @Test
    public void testBasic() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ProgressBar pb = new ProgressBar(5, null);
        for (int i = 0; i < 3 ; i++){
            pb.update();
        }
        pb.close();
        //closed so error even if not at end.
        assertThrows(IllegalStateException.class, () -> {
            pb.update();
        });
        pb.close();

    }

    @Test
    public void testToMany() {
        String description = "tooMany";
        ProgressBar pb = new ProgressBar(5, description);
        for (int i = 0; i < 5 ; i++){
            pb.update();
        }
        assertThrows(IllegalStateException.class, () -> {
            pb.update();
        });

    }

    @Test
    public void testSimple() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Easiest";
        ProgressBar pb = new ProgressBar(5, description, ps);
        for (int i = 0; i < 5 ; i++){
            pb.update();
        }
        pb.close();
        String lines[] = baos.toString().split("\\r?\\n");
        assertEquals(4, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(DASHES, lines[2]);
    }

    @Test
    public void testNoDivSmall() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Thirteen";
        try (ProgressBar pb = new ProgressBar(13, description, ps);) {
            for (int i = 0; i < 13 ; i++){
                pb.update();
            }
        }
        String lines[] = baos.toString().split("\\r?\\n");
        // Include duration as try calls close.
        assertEquals(4, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(DASHES, lines[2]);
    }

    @Test
    public void testNoDivBig() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String description = "Big";
        ProgressBar pb = new ProgressBar(133, description, ps);
        for (int i = 0; i < 133 ; i++){
            pb.update();
        }
        String lines[] = baos.toString().split("\\r?\\n");
        // No close so no duration
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
        ProgressBar pb = new ProgressBar(10, description, ps);
        for (int i = 0; i < 3 ; i++){
            pb.update();
        }
        pb.close();
        String lines[] = baos.toString().split("\\r?\\n");
        assertEquals(4, lines.length);
        assertEquals(description, lines[0]);
        assertEquals(PERCENTS, lines[1]);
        assertEquals(60 / 10 * 3 + 1, lines[2].length());
    }
}
