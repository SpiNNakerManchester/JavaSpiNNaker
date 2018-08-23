/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.storage;

import java.io.Closeable;

/**
 *
 * @author Christian-B
 */
public interface AbstractBufferedDataStorage extends Closeable {

    /**
     * Store data in the data storage in the position indicated by the write
     *      pointer index.
     *
     * @param data the data to be stored
     */
    public abstract void write(byte[] data);

    /**
     * Read data from the data storage from the position indicated by the read
     *      pointer index.
     *
     * @param dataSize number of bytes to be read
     * @return a bytearray containing the data read
     */
    public abstract byte[] read(int dataSize);

    /**
     * Read some bytes of data from the underlying storage into a predefined
     *     array.
     *
     * May block until some bytes are available, but may not fill the array
     *     completely.
     *
     * @param data The place where the data is to be stored
     * @return The number of bytes stored in data
     */
    public abstract int readinto(byte[] data);

    /**
     * Read all the data contained in the data storage starting from position 0
     *      to the end.
     *
     * @return a bytearray containing the data read
     */
    public abstract byte[] readAll();

    /**
     * Set the data storage's current read position to the offset.
     * <p>
     * The behaviour is undefined if the offset (adjusted by the whence)
     * would be either negative or too large.
     *
     * @param offset Position of the read pointer within the buffer
     * @param whence How to interpret offset
     */
    public default void seekRead(int offset, SEEK_TYPE whence) {
        seekRead(seek(offset, whence));
    }

    /**
     * Set the data storage's current read position to the offset.
     * <p>
     * The behaviour is undefined if the offset is negative or too large.
     *
     * @param offset Position of the read pointer within the buffer
     */
    public abstract void seekRead(int offset);

    /**
     * Set the data storage's current write position to the offset.
     * <p>
     * The behaviour is undefined if the offset (adjusted by the whence)
     * would be either negative or too large.
     *
     * @param offset Position of the read pointer within the buffer
     * @param whence How to interpret offset
     */
    public default void seekWrite(int offset, SEEK_TYPE whence) {
        seekWrite(seek(offset, whence));
    }

    /**
     * Set the data storage's current read position to the offset.
     * <p>
     * The behaviour is undefined if the offset is negative or too large.
     *
     * @param offset Position of the read pointer within the buffer
     */
    public abstract void seekWrite(int offset);

    /**
     * Combines the offset and seek type into an offset.
     * <p>
     * The behaviour is undefined if the offset (adjusted by the whence)
     * would be either negative or too large.
     *
     * @param offset
     * @param whence
     * @return
     */
    default int seek(int offset, SEEK_TYPE whence) {
        switch (whence) {
            case SEEK_SET:
                return offset;
            case SEEK_READ:
                return tellRead() + offset;
            case SEEK_WRITE:
                return tellWrite() + offset;
            case SEEK_END:
                return size() + offset;
            default:
                throw new IllegalArgumentException(
                        "Unexpected whence " + whence);
        }
    }

    /**
     * The current position of the read pointer.
     *
     * @return The current position of the read pointer
     */
    public abstract int tellRead();

    /**
     * The current position of the write pointer.
     *
     * @return The current position of the write pointer
     */
    public abstract int tellWrite();

    /**
     * The current size of the buffer.
     *
     * @return the current size of the buffer.
     */
    public abstract int size();

    /**
     * Check if the read pointer is a the end of the data storage.
     *
     * @return Whether the read pointer is at the end of the data storage
     */
    public default boolean eof() {
        return tellRead() >= size();
    }

}
