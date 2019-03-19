/*
 * Copyright (c) 2018 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.manchester.spinnaker.front_end.download.storage_objects;

import java.nio.ByteBuffer;

/**
 * Stores information related to a single channel output buffering state,
 * as it is retrieved at the end of a simulation on the SpiNNaker system.
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
	 * <p>
	 * 4 for {@code startAddress}, 4 for {@code currentWrite}, 4 for
	 * {@code currentDmaWrite}, 4 for {@code currentRead}, 4 for
	 * {@code endAddress}, 1 for {@code regionId}, 1 for {@code missingInfo}, 1
	 * for {@code lastBufferOperation},
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
