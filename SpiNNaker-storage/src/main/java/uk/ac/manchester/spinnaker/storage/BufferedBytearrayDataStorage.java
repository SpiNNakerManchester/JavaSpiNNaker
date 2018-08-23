/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.storage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 * @author Christian-B
 */
public class BufferedBytearrayDataStorage implements AbstractBufferedDataStorage {

    static final Logger log = getLogger(AbstractBufferedDataStorage.class);
    static final int CAPACITY_ADJUST = 10;

    private byte[] dataStorage = {};
    private int readPointer;
    private int writePointer;
    private int dataSize;

    public BufferedBytearrayDataStorage() {
        readPointer = 0;
        writePointer = 0;
    }

    @Override
    public void write(byte[] data) {
        ensureCapacity(writePointer + data.length);
        System.arraycopy(data, writePointer, dataStorage, 0, data.length);
        writePointer += data.length;
        if (dataSize < writePointer) {
            dataSize = writePointer;
        }
    }

    private void ensureCapacity(int required) {
        if (dataStorage.length < required) {
            dataStorage = Arrays.copyOf(dataStorage, required);
        }
    }

    @Override
    public byte[] read(int dataSize) {
        //byte[] result = Arrays.copyOfRange(dataStorage, readPointer, dataSize);
        //readPointer += dataSize;
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int readinto(byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte[] readAll() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private int adjustOffset(int offset) {
        if (offset < 0) {
            log.warn("Negative offset " + offset + " rounded to zero.");
            return 0;
        }
        if (offset > size()) {
            log.warn("Offset of " + offset + " is beyond the end of the data"
                    + " so adjusted back to " + size());
            return size();
        }
        return offset;
    }

    @Override
    public void seekRead(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seekWrite(int offset) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int tellRead() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int tellWrite() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void close() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }


}
