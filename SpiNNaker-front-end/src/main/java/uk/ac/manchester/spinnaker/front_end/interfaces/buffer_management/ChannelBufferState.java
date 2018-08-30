/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

/**
 *
 * @author Christian-B
 */
class ChannelBufferState {

    /**
     * Size of the state.
     *
     * 4 for _start_address, 4 for _current_write, 4 for current_dma_write,
     * 4 for _current_read, 4 for _end_address, 1 for _region_id,
     * 1 for _missing_info, 1 for _last_buffer_operation,
     */
    public static final int STATE_SIZE = 24;


}
