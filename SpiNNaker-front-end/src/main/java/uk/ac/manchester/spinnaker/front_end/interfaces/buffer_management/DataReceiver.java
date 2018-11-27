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
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.BufferedReceivingData;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.BufferingOperation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import static uk.ac.manchester.spinnaker.messages.Constants.WORD_SIZE;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.processes.ProcessException;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

/**
 * Stripped down version of the BufferManager for early testing.
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/buffer_manager.py">
 * Python Version</a>

 * @author Christian-B
 */
public class DataReceiver {

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
    //      buffer_management/recording_utilities.py
    /** The offset of the last sequence number field in bytes. */
    private static final int LAST_SEQUENCE_NUMBER_OFFSET = 4 * 6;

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
    //      buffer_management/recording_utilities.py
    /** The offset of the memory addresses in bytes. */
    private static final int FIRST_REGION_ADDRESS_OFFSET = 4 * 7;

    private final Transceiver transceiver;
    private final BufferedReceivingData receivedData;

    private static final Logger log = getLogger(DataReceiver.class);

    /**
     * Creates a new mini BufferManager.
     *
     * @param tranceiver Transceiver to get data from.
     * @param databasePath The path of a file that contains
     *      an SQLite database holding the data.
     */
    public DataReceiver(Transceiver tranceiver, String databasePath) {

        this.transceiver = tranceiver;
        // storage area for received data from cores
        receivedData = new BufferedReceivingData(databasePath);

    }

	/**
	 * Gets the data for a list of placements.
	 * <p>
	 * Note: This method is subject to change as best way to pass in placement
	 * data is determined.
	 *
	 * @param placements
	 *            List of placements.
	 * @param progress
	 *            progressBar if used
	 * @throws IOException
	 *             if communications fail
	 * @throws ProcessException
	 *             if SpiNNaker rejects a message
	 * @throws StorageException
	 *             if database access fails
	 */
    public void getDataForPlacements(
            List<Placement> placements, ProgressBar progress)
            throws IOException, StorageException, ProcessException {
        // TODO  with self._thread_lock_buffer_out:
        getDataForPlacementsLocked(placements, progress);
    }

    private void getDataForPlacementsLocked(
            List<Placement> placements, ProgressBar progress)
            throws IOException, StorageException, ProcessException {

        // get data
        for (Placement placement:  placements) {
            for (int recordingRegionId
                    : placement.vertex.getRecordedRegionIds()) {
                getDataForPlacement(placement, recordingRegionId);
                if (progress != null) {
                    progress.update();
                }
            }
        }
    }

    private void getDataForPlacement(
            Placement placement, int recordingRegionId)
            throws IOException, StorageException, ProcessException {
        // TODO with self._thread_lock_buffer_out:
        getDataForPlacementLocked(placement, recordingRegionId);
    }

    // This is only the simple case of the full method in BufferManager
    private void getDataForPlacementLocked(
            Placement placement, int recordingRegionId)
            throws IOException, StorageException, ProcessException {

        Vertex vertex = placement.getVertex();
        int recordingDataAddress = vertex.getRecordingRegionBaseAddress();
        // Combine placement.x, placement.y, placement.p,  recording_region_id
        RegionLocation location = new RegionLocation(
                placement, recordingRegionId);

        // Ensure the last sequence number sent has been retrieved
        if (!receivedData.isEndBufferingSequenceNumberStored(placement)) {
            receivedData.storeEndBufferingSequenceNumber(placement,
                getLastSequenceNumber(placement, recordingDataAddress));
        }

        // Read the data if not already received
        if (!receivedData.isDataFromRegionFlushed(location)) {

            // Read the end state of the recording for this region
            ChannelBufferState endState;
            if (!receivedData.isEndBufferingStateRecovered(location)) {
                int regionPointer = this.getRegionPointer(
                        placement, recordingDataAddress, recordingRegionId);
                endState = generateEndBufferingStateFromMachine(
                        placement, regionPointer);
                receivedData.storeEndBufferingState(location, endState);
            } else {
                endState = receivedData.getEndBufferingState(location);
            }


            // current read needs to be adjusted in case the last portion of the
            // memory has already been read, but the HostDataRead packet has not
            // been processed by the chip before simulation finished.
            // This situation is identified by the sequence number of the last
            // packet sent to this core and the core internal state of the
            // output buffering finite state machine
            Integer seqNoLastAckPacket = receivedData.lastSequenceNoForCore(
                    placement);

            // get the sequence number the core was expecting to see next
            Integer coreNextSequenceNumber =
                    receivedData.getEndBufferingSequenceNumber(placement);

            // if the core was expecting to see our last sent sequence,
            // it must not have received it
            if (coreNextSequenceNumber == seqNoLastAckPacket) {
                throw new UnsupportedOperationException("Not supported yet.");
                //processLastAck(location, endState);
            }

            // now state is updated, read back values for read pointer and
            // last operation performed
            BufferingOperation lastOperation =
                    endState.getLastBufferOperation();

            if (endState.getCurrentRead() <  endState.currentWrite) {
                int length =  endState.currentWrite - endState.getCurrentRead();
                readSomeData(location, endState.getCurrentRead(), length);
            } else if (endState.getCurrentRead() >  endState.currentWrite
                    || endState.getLastBufferOperation()
                    == BufferingOperation.BUFFER_WRITE) {
                int length = endState.endAddress - endState.getCurrentRead();
                if (length < 0) {
                    throw new IOException(
                            "The amount of data to read is negative!");
                }
                readSomeData(location, endState.getCurrentRead(), length);
                length = endState.currentWrite - endState.startAddress;
                readSomeData(location, endState.startAddress, length);
            } else {
               ByteBuffer data = ByteBuffer.allocate(0);
               receivedData.flushingDataFromRegion(location, data);
            }
        }
    }

    private void readSomeData(RegionLocation location, int address, int length)
            throws IOException, StorageException, ProcessException {
        log.debug("< Reading " + length + " bytes from "
            + location + " at " + address);
        ByteBuffer data = requestData(location, address, length);
        receivedData.flushingDataFromRegion(location, data);
    }

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
    //    buffer_management/recording_utilities.py
    private int getLastSequenceNumber(
            Placement placement, int recordingDataAddress)
            throws IOException, ProcessException {
        ByteBuffer data = transceiver.readMemory(
                placement.getScampCore(),
                recordingDataAddress + LAST_SEQUENCE_NUMBER_OFFSET, WORD_SIZE);
        int num =  data.getInt(0);
        return num;
    }

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/
    //     buffer_management/recording_utilities.py
    /**
     * Get a pointer to a recording region.
     *
     * @param placement The placement from which to read the pointer
     * @param recording_data_address
     *      The address of the recording data from which to read the pointer
     * @param region
     *      The index of the region to get the pointer of
     * @return The index of the region to get the pointer of.
     * @throws IOException if communications fail
     */
    private int getRegionPointer(
            Placement placement, int recordingDataAddress, int region)
            throws IOException, ProcessException {
        ByteBuffer data = transceiver.readMemory(
                placement.getScampCore(),
                recordingDataAddress + FIRST_REGION_ADDRESS_OFFSET
                        + (region * WORD_SIZE), WORD_SIZE);
        return data.getInt(0);
    }

    private ChannelBufferState generateEndBufferingStateFromMachine(
            Placement placement, int stateRegionBaseAddress)
            throws IOException, ProcessException {
        // retrieve channel state memory area
        ByteBuffer channelStateData = requestData(
                placement, stateRegionBaseAddress,
                ChannelBufferState.STATE_SIZE);
        return new ChannelBufferState(channelStateData);
    }

    /**
     * Uses the extra monitor cores for data extraction.
     *
     * @param placement
     *      The placement coords where data is to be extracted from.
     * @param address
     *      The memory address to start at
     * @param length
     *      The number of bytes to extract
     * @return
     *      data as a byte array
     * @throws IOException if communications fail
     */
    private ByteBuffer requestData(
            HasCoreLocation location, int address, int length)
            throws IOException, ProcessException {
        return transceiver.readMemory(location.getScampCore(), address, length);
    }

/*    private void processLastAck(
    RegionLocation location, ChannelBufferState endState) {
        // if the last ACK packet has not been processed on the chip,
        // process it now
        // TODO python retrieves the value and then overwrites it but WHY!
        HostDataRead lastSentAck = receivedData.lastSentPacketToCore(location);

        EIEIOMessageFactory.readCommandMessage(lastSentAck);


        lastSentAck = create_eieio_command.read_eieio_command_message(
            last_sent_ack.data, 0)
        if not isinstance(last_sent_ack, HostDataRead):
            raise Exception(
                "Something somewhere went terribly wrong; looking for a "
                "HostDataRead packet, while I got {0:s}".format(last_sent_ack))

        start_ptr = end_state.start_address
        write_ptr = end_state.current_write
        end_ptr = end_state.end_address
        read_ptr = end_state.current_read

        for i in xrange(last_sent_ack.n_requests):
            in_region = region_id == last_sent_ack.region_id(i)
            if in_region and not end_state.is_state_updated:
                read_ptr += last_sent_ack.space_read(i)
                if (read_ptr == write_ptr or
                        (read_ptr == end_ptr and write_ptr == start_ptr)):
                    end_state.update_last_operation(
                        BUFFERING_OPERATIONS.BUFFER_READ.value)
                if read_ptr == end_ptr:
                    read_ptr = start_ptr
                elif read_ptr > end_ptr:
                    raise Exception(
                        "Something somewhere went terribly wrong; I was "
                        "reading beyond the region area")
        end_state.update_read_pointer(read_ptr)
        end_state.set_update_completed()

    }
*/
}
