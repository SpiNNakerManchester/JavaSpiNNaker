/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.BufferingOperation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.HasCoreLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.RegionLocation;
import uk.ac.manchester.spinnaker.storage.StorageException;
import uk.ac.manchester.spinnaker.transceiver.SpinnmanException;
import uk.ac.manchester.spinnaker.transceiver.processes.Process;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

/**
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/buffer_manager.py">
 * Python Version</a>

 * @author Christian-B
 */
public class DataReceiver {

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    // The offset of the last sequence number field in bytes
    private static final int LAST_SEQUENCE_NUMBER_OFFSET = 4 * 6;

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    // The offset of the memory addresses in bytes
    private static final int FIRST_REGION_ADDRESS_OFFSET = 4 * 7;

    private final Transceiver transceiver;
    private final BufferedReceivingData receivedData;

    private static final Logger log = getLogger(DataReceiver.class);

    public DataReceiver(Transceiver tranceiver, String databasePath) throws IOException, SpinnmanException, Process.Exception {

        this.transceiver = tranceiver;
        // storage area for received data from cores
        receivedData = new BufferedReceivingData(databasePath);

    }

    public void getDataForPlacements(List<Placement> placements, ProgressBar progress) throws IOException, Process.Exception, StorageException{
        // TODO  with self._thread_lock_buffer_out:
        getDataForPlacementsLocked(placements, progress);
    }

    //TODO Object type
    private void getDataForPlacement(Placement placement, int recordingRegionId) throws IOException, Process.Exception, StorageException {
        // TODO with self._thread_lock_buffer_out:
        getDataForPlacementLocked(placement, recordingRegionId);
    }

    private void getDataForPlacementsLocked(List<Placement> placements, ProgressBar progress) throws IOException, Process.Exception, StorageException {

        // get data
        for (Placement placement:  placements) {
            for (int recordingRegionId : placement.vertex.getRecordedRegionIds()) {
                getDataForPlacement(placement, recordingRegionId);
                if (progress != null) {
                    progress.update();
                }
            }
        }
    }

    private void readSomeData(RegionLocation location, int address, int length)
            throws IOException, Process.Exception, StorageException {
        log.debug("< Reading " + length + " bytes from "
            + location + " at " + address);
        ByteBuffer data = requestData(location, address, length);
        receivedData.flushingDataFromRegion(location, data);
    }

    private void getDataForPlacementLocked(Placement placement, int recordingRegionId) throws IOException, Process.Exception, StorageException {

        Vertex vertex = placement.getVertex();
        int recordingDataAddress = vertex.getRecordingRegionBaseAddress();
        // Combine placement.x, placement.y, placement.p,  recording_region_id
        RegionLocation location = new RegionLocation(placement, recordingRegionId);

        // TODO Just because we have A sequence number can we assume it is the last one?
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
                int regionPointer = this.getRegionPointer(placement, recordingDataAddress, recordingRegionId);
                endState = generateEndBufferingStateFromMachine(placement, regionPointer);
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
            Integer seqNoLastAckPacket = receivedData.lastSequenceNoForCore(placement);

            // get the sequence number the core was expecting to see next
            Integer coreNextSequenceNumber = receivedData.getEndBufferingSequenceNumber(placement);

            // if the core was expecting to see our last sent sequence,
            // it must not have received it
            if (coreNextSequenceNumber == seqNoLastAckPacket) {
                throw new UnsupportedOperationException("Not supported yet.");
                //processLastAck(location, endState);
            }

            // now state is updated, read back values for read pointer and
            // last operation performed
            BufferingOperation lastOperation = endState.getLastBufferOperation();
            //int start_ptr = endState.startAddress;
            //int end_ptr = endState.endAddress;
            //int write_ptr = endState.currentWrite;
            //int read_ptr = endState.currentRead;


            if (endState.getCurrentRead() <  endState.currentWrite) {
                int length =  endState.currentWrite - endState.getCurrentRead();
                readSomeData(location, endState.getCurrentRead(), length);
            } else if (endState.getCurrentRead() >  endState.currentWrite ||
                    endState.getLastBufferOperation() == BufferingOperation.BUFFER_WRITE) {
                int length = endState.endAddress - endState.getCurrentRead();
                if (length < 0) {
                    throw new IOException ("The amount of data to read is negative!");
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

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    private int getLastSequenceNumber(Placement placement, int recordingDataAddress) throws IOException, Process.Exception {
        ByteBuffer data = transceiver.readMemory(placement.getScampCore(), recordingDataAddress + LAST_SEQUENCE_NUMBER_OFFSET, 4);
        int num =  data.getInt(0);
        return num;
    }

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    /**
     * Get a pointer to a recording region.
     *
     * @param placement The placement from which to read the pointer
     * @param recording_data_address
     *      The address of the recording data from which to read the pointer
     * @param region
     *      The index of the region to get the pointer of
     * @return
     */
    private int getRegionPointer(Placement placement, int recordingDataAddress, int region) throws IOException, Process.Exception {
        ByteBuffer data = transceiver.readMemory(placement.getScampCore(), recordingDataAddress + FIRST_REGION_ADDRESS_OFFSET + (region * 4), 4);
        return data.getInt(0);
    }

    private ChannelBufferState generateEndBufferingStateFromMachine(Placement placement, int state_region_base_address) throws IOException, Process.Exception {
        // retrieve channel state memory area
        ByteBuffer channelStateData = requestData(
                placement, state_region_base_address,
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
     */
    private ByteBuffer requestData(HasCoreLocation location, int address, int length) throws IOException, Process.Exception {
        return transceiver.readMemory(location.getScampCore(), address, length);
    }

/*    private void processLastAck(RegionLocation location, ChannelBufferState endState) {
        // if the last ACK packet has not been processed on the chip,
        // process it now
        // TODO python retreiuves the value and then overwrites it but WHY!
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
