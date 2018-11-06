/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.nio.ByteBuffer;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Christian-B
 */
public class TestByteBufferUtils {

    @Test
    public void testHexRead() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        assertEquals("(10, 15) 68,65,6c,6c,6f,77,6f,72,6c,64", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testHexWrite() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        assertEquals("(0, 10) 68,65,6c,6c,6f,77,6f,72,6c,64", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testHexDouble() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        bb.flip();
        assertEquals("(0, 0) ", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testFullRead() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        assertEquals("(15, 15) 68,65,6c,6c,6f,77,6f,72,6c,64,31,32,33,34,35", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testFullWrite() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        assertEquals("(0, 15) 68,65,6c,6c,6f,77,6f,72,6c,64,31,32,33,34,35", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testFullDouble() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        bb.flip();
        assertEquals("(0, 0) ", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testEmpty() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        //Impossible to tell if unfliped empty or flipped full.
        assertThat(ByteBufferUtils.asString(bb), startsWith("(0, 15)"));
    }

    @Test
    public void testEmptyFlipped() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.flip();
        assertEquals("(0, 0) ", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testEmptyFlipped2() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.flip();
        bb.flip();
        assertEquals("(0, 0) ", ByteBufferUtils.asString(bb));
    }
}
