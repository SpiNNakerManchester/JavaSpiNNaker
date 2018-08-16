/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.net.URL;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestRawConfigParser {

    public TestRawConfigParser() {
    }

    @Test
    public void testSimple() {
        URL url = TestRawConfigParser.class.getResource("/testconfig/test.cfg");
        RawConfigParser parser = new RawConfigParser(url);
        assertEquals((Integer)5, parser.getint("Machine", "version"));
        assertTrue(parser.getboolean("Other", "alan_is_scotish"));
    }



}
