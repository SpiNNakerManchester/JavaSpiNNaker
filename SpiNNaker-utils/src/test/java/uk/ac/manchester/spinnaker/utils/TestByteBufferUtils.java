/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestByteBufferUtils {
    @Test
    public void testHex() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        assertEquals("68,65,6c,6c,6f,77,6f,72,6c,64", ByteBufferUtils.byteArrayToHex(bb));
    }


}
