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
        System.out.println("10 of 15 not");
        assertEquals("68,65,6c,6c,6f,77,6f,72,6c,64", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testHexWrite() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        System.out.println("10 of 15 flipped");
        assertEquals("68,65,6c,6c,6f,77,6f,72,6c,64", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testHexDouble() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        bb.flip();
        System.out.println("10 of 15 double");
        assertEquals("", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testFullRead() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        System.out.println("15 of 15 not");
        assertEquals("68,65,6c,6c,6f,77,6f,72,6c,64,31,32,33,34,35", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testFullWrite() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        System.out.println("15 of 15 flipped");
        assertEquals("68,65,6c,6c,6f,77,6f,72,6c,64,31,32,33,34,35", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testFullDouble() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        for (byte b: "helloworld12345".getBytes()) {
            bb.put(b);
        }
        bb.flip();
        bb.flip();
        System.out.println("15 of 15 double");
        assertEquals("", ByteBufferUtils.asHex(bb));
    }

    @Test
    public void testEmpty() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        System.out.println("empty not");
        //Impossible to tell if unfliped empty or flipped full.
        assertThat(ByteBufferUtils.asString(bb), startsWith("(0)"));
    }

    @Test
    public void testEmptyFlipped() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.flip();
        System.out.println("empty flipped");
        assertEquals("(0)", ByteBufferUtils.asString(bb));
    }

    @Test
    public void testEmptyFlipped2() {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.flip();
        bb.flip();
        System.out.println("empty flipped2");
        //Impossible to tell if unfliped empty or flipped full.
        assertThat(ByteBufferUtils.asString(bb), startsWith("(0)"));
    }
}
