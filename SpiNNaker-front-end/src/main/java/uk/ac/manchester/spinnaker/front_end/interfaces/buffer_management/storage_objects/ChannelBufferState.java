/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects;

import java.nio.ByteBuffer;

/**
 * Stores information related to a single channel output buffering state,
 *     as it is retrieved at the end of a simulation on the SpiNNaker system.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/storage_objects/channel_buffer_state.py">
 * Python Version</a>
 * @author Christian-B
 */
public class ChannelBufferState {

    /** start buffering area memory address (32 bits). */
    public final int startAddress;

    /** address where data was last written (32 bits). */
    public final int  currentWrite;

    /** address where the DMA write got up to (32 bits). */
    public final int currentDmaWrite;

    /** address where data was last read (32 bits). */
    private int currentRead;

    /** The address of first byte after the buffer (32 bits). */
    public final int endAddress;

    /** The ID of the region (8 bits). */
    public final byte regionId;

    /** True if the region overflowed during the simulation (8 bits). */
    public final byte missingInfo;

    /** Last operation performed on the buffer - read or write (8 bits). */
    private BufferingOperation lastBufferOperation;

    /** bool check for if its extracted data from machine. */
    private boolean updateCompleted;

    /**
     * Size of the state.
     *
     * 4 for _start_address, 4 for _current_write, 4 for current_dma_write,
     * 4 for _current_read, 4 for _end_address, 1 for _region_id,
     * 1 for _missing_info, 1 for _last_buffer_operation,
     */
    public static final int STATE_SIZE = 24;

    /**
     * Creates a new ChannelBufferState reading from the ByteBuffer.
     *
     * @param data ByteBuffer assumed to hold channel buffer state.
     */
    public ChannelBufferState(ByteBuffer data) {
        // _CHANNEL_BUFFER_PATTERN = struct.Struct("<IIIIIBBBx")
        startAddress = data.getInt();
        currentWrite = data.getInt();
        currentDmaWrite = data.getInt();
        currentRead =  data.getInt();
        endAddress =  data.getInt();
        regionId = data.get();
        missingInfo = data.get();
        byte lastBuffer = data.get();
        if (lastBuffer == 0) {
            lastBufferOperation = BufferingOperation.BUFFER_READ;
        } else {
            lastBufferOperation = BufferingOperation.BUFFER_WRITE;
        }
        // x  IE move the read flag to ignore the last byte
        data.get();
        updateCompleted = false;
    }

    /**
     * @return the currentRead
     */
    public int getCurrentRead() {
        return currentRead;
    }

    /**
     * @param currentRead the currentRead to set
     */
    public void setCurrentRead(int currentRead) {
        this.currentRead = currentRead;
    }

    /**
     * @return the lastBufferOperation
     */
    public BufferingOperation getLastBufferOperation() {
        return lastBufferOperation;
    }

    /**
     * @param lastBufferOperation the lastBufferOperation to set
     */
    public void setLastBufferOperation(BufferingOperation lastBufferOperation) {
        this.lastBufferOperation = lastBufferOperation;
    }

    /**
     * @return the updateCompleted
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * @param updateCompleted the updateCompleted to set
     */
    public void setUpdateCompleted(boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }
}
