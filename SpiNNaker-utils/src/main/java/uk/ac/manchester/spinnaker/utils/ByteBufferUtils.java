/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.utils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * @author Christian-B
 */
public class ByteBufferUtils {


    public static String asString(ByteBuffer bb) {
        return "(" + bb.position() + ")" + asHex(bb);
    }

    public static String asString(ByteBuffer bb, int from) {
        return asString(bb, from, bb.position());
    }

    public static String asString(ByteBuffer bb, int from, int to) {
        return "(" + bb.position() + ")" + asHex(bb, from, to);
    }

    public static String asHex(ByteBuffer bb) {
        ByteBuffer duplicate = bb.duplicate();
        if (duplicate.position() > 0) {
            duplicate.flip();
        }
        StringBuilder sb = new StringBuilder();
        while (duplicate.hasRemaining()){
            sb.append(String.format("%02x,", duplicate.get()));
        }
        if (sb.length() > 1) {
            // remove the last comma
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static String byteArrayToHex(ByteBuffer bb, int start) {
        return asHex(bb, 0, start);
    }

    public static String asHex(ByteBuffer bb, int from, int to) {
        return asHex(Arrays.copyOfRange(bb.array(), from, to));
    }

    public static String asHex(byte[] ba) {
        StringBuilder sb = new StringBuilder();
        for(byte b: ba)
            sb.append(String.format("%02x,", b));
        // remove the last comma
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

}
