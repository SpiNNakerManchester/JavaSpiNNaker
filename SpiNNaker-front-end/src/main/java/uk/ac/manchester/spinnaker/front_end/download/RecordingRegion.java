package uk.ac.manchester.spinnaker.front_end.download;

import java.nio.ByteBuffer;

/**
 * Information about a channel of recording data.
 *
 * Must match {@code recording_region_t} in {@code recording.c}
 * in SpiNNFrontEndCommon.
 *
 */
public class RecordingRegion {

    /**
     * Size of the channel data on the machine.
     * <p>
     * 4 for {@code space}, 4 for {@code missing_flag + size},
     * 4 for {@code data}
     */
    public static final int SIZE = 12;

    /**
     * Mask of flag to indicate missing data.
     */
    private static final long MISSING_MASK = 0x80000000L;

    /**
     * Mask of size.
     */
    private static final long SIZE_MASK = 0x7FFFFFFFL;

    /**
     * The size of the space available. (32-bits; unsigned)
     */
    public final long space;

    /**
     * If there is any missing information. (1-bit)
     */
    public final boolean missing;

    /**
     * The size of the recording in bytes. (31-bits; unsigned)
     */
    public final long size;

    /**
     * The data memory address. (32-bits; unsigned)
     */
    public final long data;

    RecordingRegion(ByteBuffer buffer) {
        space = Integer.toUnsignedLong(buffer.getInt());
        long missingAndSize = Integer.toUnsignedLong(buffer.getInt());
        missing = (missingAndSize & MISSING_MASK) > 0;
        size = missingAndSize & SIZE_MASK;
        data = Integer.toUnsignedLong(buffer.getInt());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Recording Channel:{");
        sb.append("space:").append(space);
        sb.append(",size:").append(size);
        sb.append(",data:0x").append(Long.toHexString(data));
        if (missing) {
            sb.append(",missing");
        }
        return sb.append("}").toString();
    }

}
