/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management.storage_objects.ChannelBufferState;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;
import uk.ac.manchester.spinnaker.machine.Machine;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifer;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIODataMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOPrefix;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOType;
import uk.ac.manchester.spinnaker.messages.eieio.EventStopRequest;
import uk.ac.manchester.spinnaker.messages.eieio.PaddingRequest;
import uk.ac.manchester.spinnaker.processes.Process;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.UDPTransceiver;
import uk.ac.manchester.spinnaker.utils.DefaultMap;
import uk.ac.manchester.spinnaker.utils.progress.ProgressBar;

/**
 *
 * @see <a href=
 * "https://github.com/SpiNNakerManchester/SpiNNFrontEndCommon/blob/master/spinn_front_end_common/interface/buffer_management/buffer_manager.py">
 * Python Version</a>

 * @author Christian-B
 */
public class BufferManager {

    //Found in DataSpecification/data_specification/constants.py
    private static final int APP_PTR_TABLE_HEADER_BYTE_SIZE = 8;

    //2 bytes of generic EIEIO header, 4 bytes of key (for 32 bits)
    //and 0 bytes of payload (as it is KEY_32_BIT, not KEY_PAYLOAD_32_BIT).
    private static final int MIN_MESSAGE_SIZE = 6;

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    // The offset of the last sequence number field in bytes
    private static final int LAST_SEQUENCE_NUMBER_OFFSET = 4 * 6;

    // found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    // The offset of the memory addresses in bytes
    private static final int FIRST_REGION_ADDRESS_OFFSET = 4 * 7;

    private final Placements placements;
    private final Tags tags;
    private final Transceiver transceiver;
    private final DefaultMap<InetAddress, HashSet> seenTags = new DefaultMap<>(HashSet::new);
    private final HashSet<Vertex> senderVertices;
    private final HashMap<Vertex,BuffersSentDeque> sentMessages;
    private final BufferedReceivingData receivedData;
        //# Lock to avoid multiple messages being processed at the same time
        //"_thread_lock_buffer_out",

        //# Lock to avoid multiple messages being processed at the same time
        //"_thread_lock_buffer_in",

    volatile boolean finished;

    private Integer listenerPort;  //may be null

        //# Store to file flag
        //"_store_to_file",

        //# Buffering out thread pool
        //"_buffering_out_thread_pool",

    // TODO do we need this or can we not just do extraMonitorCoresByChip.values()
    // # the extra monitor cores which support faster data extraction
    private final List<ExtraMonitorSupportMachineVertex> extraMonitorCores;

    //# the extra_monitor to Ethernet connection map
    private final Map<ChipLocation, DataSpeedUpPacketGatherMachineVertex> extraMonitorCoresToEthernetConnectionMap;

    //# monitor cores via chip ID
    private final Map<ChipLocation, ExtraMonitorSupportMachineVertex> extraMonitorCoresByChip;


        //"_extra_monitor_cores_by_chip",

    //# fixed routes, used by the speed up functionality for reports
    private final Map<CoreLocation, FixedRouteEntry> fixedRoutes;

    //# machine object
    private final Machine machine;

    // flag for what data extraction to use
    private final  boolean usesAdvancedMonitors;

	private static final Logger log = getLogger(BufferManager.class);

    public BufferManager(Placements placements, Tags tags, Transceiver tranceiver,
            List<ExtraMonitorSupportMachineVertex> extraMonitorCores,
            Map<ChipLocation, DataSpeedUpPacketGatherMachineVertex> extraMonitorCoresToEthernetConnectionMap,
            Map<ChipLocation, ExtraMonitorSupportMachineVertex> extraMonitorCoresByChip,
            Machine machine, Map<CoreLocation, FixedRouteEntry> fixedRoutes,
            boolean usesAdvancedMonitors, boolean storeToFile) {
        this.placements = placements;
        this.tags = tags;
        this.transceiver = tranceiver;
        this.extraMonitorCores = extraMonitorCores;
        this.extraMonitorCoresToEthernetConnectionMap = extraMonitorCoresToEthernetConnectionMap;
        this.extraMonitorCoresByChip = extraMonitorCoresByChip;
        this.fixedRoutes = fixedRoutes;
        this.machine = machine;
        this.usesAdvancedMonitors = usesAdvancedMonitors;

        //# Set of (ip_address, port) that are being listened to for the tags
        // self._seen_tags = set()

        // Set of vertices with buffers to be sent
        senderVertices = new HashSet<>();

        // Dictionary of sender vertex -> buffers sent
        sentMessages = new HashMap<>();

        // storage area for received data from cores
        receivedData = new BufferedReceivingData(storeToFile);

        //self._store_to_file = store_to_file

        //# Lock to avoid multiple messages being processed at the same time
        //self._thread_lock_buffer_out = threading.RLock()
        //self._thread_lock_buffer_in = threading.RLock()
        //self._buffering_out_thread_pool = ThreadPool(processes=1)

        finished = false;
        listenerPort = null;
    }

    //def __init__(self, placements, tags, transceiver, extra_monitor_cores,
    //             extra_monitor_cores_to_ethernet_connection_map,
    //             extra_monitor_to_chip_mapping, machine, fixed_routes,
    //             uses_advanced_monitors, store_to_file=False)

    public void addReceivingVertex(Vertex vertex) throws IOException {
        addBufferListeners(vertex);
    }

    public void addSenderVertex(Vertex vertex) throws IOException {
        senderVertices.add(vertex);
        addBufferListeners(vertex);
    }

    public void resume() {
        receivedData.resume();
        finished = false;
    }

    public void loadInitialBuffers() throws IOException, Process.Exception, BufferableRegionTooSmall {
        int totalData = 0;
        for (Vertex vertex:this.senderVertices) {
            for (Integer region:vertex.getRegions()){
                totalData += vertex.getRegionBufferSize(region);
            }
        }
        ProgressBar progress = new ProgressBar(
                totalData, "Loading buffers (" + + totalData +" bytes)");
        for (Vertex vertex:this.senderVertices) {
            for (Integer region:vertex.getRegions()){
                sendInitialMessages(vertex, region, progress);
            }
        }

        progress.close();
    }

    private void addBufferListeners(Vertex vertex) throws IOException {
        for (IPTag tag:tags.getIpTagsForVertex(vertex)) {
            if (tag.getTrafficIdentifier() == TrafficIdentifer.BUFFERED) {
                if (tag.getPort() == null) {
                    if (listenerPort == null) {
                        UDPConnection connection = createConnection(tag);
                        listenerPort = connection.getLocalPort();
                    }
                    tag.setPort(listenerPort);
                }
            } else {
                if (!seenTags.get(tag.getIPAddress()).contains(tag.getPort())) {
                    createConnection(tag);
                }
            }
        }
    }

    //	public final <T> UDPConnection<T> registerUDPListener(
	//		MessageHandler<T> callback,
	//		ConnectionFactory<? extends UDPConnection<T>> connectionFactory,
	//		Integer localPort, String localHost) throws IOException {

    private UDPConnection createConnection(IPTag tag) throws IOException {
        MessageHandler callback = receiveBufferCommandMessage();
        UDPConnection connection = transceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory(), tag.getPort(),
                tag.getIPAddress());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress());
        return connection;
    }

    //Type of UDPConnection and MessageHandler
    private UDPConnection<EIEIOMessage<? extends EIEIOHeader>> createConnection1(IPTag tag) throws IOException {
        MessageHandler<EIEIOMessage<? extends EIEIOHeader>> callback = receiveBufferCommandMessage();
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> connectionFactory = new EIEIOConnectionFactory();
        UDPConnection<EIEIOMessage<? extends EIEIOHeader>> connection = transceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory(), tag.getPort(),
                tag.getIPAddress());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress());
        return connection;
    }

    private UDPConnection<EIEIOMessage> createConnection2(IPTag tag) throws IOException {
        MessageHandler<EIEIOMessage> callback = receiveBufferCommandMessage2();
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> connectionFactory = new EIEIOConnectionFactory();
        UDPConnection<EIEIOMessage> connection = transceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory2(), tag.getPort(),
                tag.getIPAddress());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress());
        return connection;
    }
    //        <T> UDPConnection<T> registerUDPListener(
	//		MessageHandler<T> callback,
	//		UDPTransceiver.ConnectionFactory<? extends UDPConnection<T>> connectionFactory,
	//		Integer localPort, String localHost)

    //type as createConnection(IPTag tag)
    private MessageHandler<EIEIOMessage<? extends EIEIOHeader>> receiveBufferCommandMessage() {
        return new MessageHandler() {
            @Override
            public void handle(Object message) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };
    }

    private MessageHandler<EIEIOMessage> receiveBufferCommandMessage2() {
        return new MessageHandler() {
            @Override
            public void handle(Object message) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };
    }

    private void sendInitialMessages(Vertex vertex, Integer region, ProgressBar progress) throws IOException, Process.Exception, BufferableRegionTooSmall {
        Placement placement = placements.getPlacementOfVertex(vertex);
        int regionBaseAddress = locateMemoryRegionForPlacement(placement, region, transceiver);
        // Add packets until out of space
        boolean sentMessage = false;
        int bytesToGo = vertex.getRegionBufferSize(region);
        if (bytesToGo % 2 != 0) {
            throw new Error(
                    "The buffer region of " + vertex + " must be divisible by 2");
        }
        //TODO: verify if bytesToGo is big enough (Python has no capacity)
        ByteBuffer allData = ByteBuffer.allocate(bytesToGo).order(ByteOrder.LITTLE_ENDIAN);
        if (vertex.isEmpty(region))
            sentMessage = true;
        else {
            while (vertex.isNextTimestamp(region) &&
                    bytesToGo > MIN_MESSAGE_SIZE) {
                int spaceAvailable = Math.min(bytesToGo, 280);
                EIEIODataMessage nextMessage = createMessageToSend(spaceAvailable, vertex, region);

                if (nextMessage == null) {
                    break;
                }
                // Write the message to the memory
                ByteBuffer data = nextMessage.getData();  // next_message.bytestring
                allData.put(data);
                sentMessage = true;
                // Update the positions
                //TODO verify position can be used for size
                bytesToGo -= data.position();
                progress.update(data.position());
            }
        }

        if (!sentMessage) {
            throw new BufferableRegionTooSmall(
                "The buffer size " + bytesToGo
                + " is too small for any data to be added for region "
                + region + " of vertex " + vertex);
        }


        // If there are no more messages and there is space, add a stop request
        EventStopRequest stopRequest = new EventStopRequest();
        if (! vertex.isNextTimestamp(region) && bytesToGo >= stopRequest.minPacketLength()){
            //ByteBuffer data = ByteBuffer.allocate(stopRequest.minPacketLength()).order(ByteOrder.LITTLE_ENDIAN);
            int start = allData.position();
            stopRequest.getHeader().addToBuffer(allData);
            int added = allData.position() - start;
            bytesToGo -= added;
            progress.update(added);
            sentMessages.put(vertex, new BuffersSentDeque(region, true));
        }
        // If there is any space left, add padding
        if (bytesToGo > 0) {
            PaddingRequest padding_packet = new PaddingRequest();
            //n_packets = bytes_to_go // padding_packet.get_min_packet_length()
            for (int i = 0; i < bytesToGo; i++) {
                padding_packet.addToBuffer(allData);
            }
            progress.update(bytesToGo);
        }
        // Do the writing all at once for efficiency
         transceiver.writeMemory(placement, regionBaseAddress, allData);
    }

    public void stop() {
        //Python version used lock_buffer_in and thread_lock_buffer_out
        finished = true;
    }

    public void getDataForVertices(List<Vertex> vertices, ProgressBar progress) throws IOException, Process.Exception{
        // TODO  with self._thread_lock_buffer_out:
        getDataForVerticesLocked(vertices, progress);
    }

    //TODO Object type
    private Object getDataForVertex(Placement placement, int recordingRegionId) throws IOException, Process.Exception {
        // TODO with self._thread_lock_buffer_out:
        return getDataForVertexLocked(placement, recordingRegionId);
    }

    //found in SpiNNFrontEndCommon/spinn_front_end_common/utilities/helpful_functions.py
    private int locateMemoryRegionForPlacement(Placement placement, Integer region, Transceiver tranceiver) throws IOException, Process.Exception {
        int regionsBaseAddress = tranceiver.getCPUInformation(placement).getUser()[0];
        int regionOffset = getRegionBaseAddressOffset(regionsBaseAddress, region);
        ByteBuffer regionAddress = tranceiver.readMemory(placement, regionOffset, 4);
        return regionAddress.getInt();
    }

    //Found in DataSpecification/data_specification/utility_calls.py
    private int getRegionBaseAddressOffset(int appDataBaseAddress, Integer region) {
        return appDataBaseAddress + APP_PTR_TABLE_HEADER_BYTE_SIZE + (region * 4);
    }

    private EIEIODataMessage createMessageToSend(int size, Vertex vertex, Integer region) {
        if (!vertex.isNextTimestamp(region)) {
            return null;
        }

        // Create a new message
        Integer nextTimestamp = vertex.getNextTimestamp(region);

        //payload_base = payload_prefix
        //if timestamp is not None:
        //    payload_base = timestamp
        byte count = 0;
        ByteBuffer data = null;
        Short keyPrefix = null;
        Integer timestamp = null;
        EIEIOPrefix prefixType = EIEIOPrefix.LOWER_HALF_WORD;
        EIEIODataMessage message = new EIEIODataMessage(EIEIOType.KEY_32_BIT, count, data,
			keyPrefix, nextTimestamp, timestamp, prefixType);

        // If there is no room for the message, return None
        // TODO: Work out if this ever happens
        if (message.getSize() + EIEIOType.KEY_32_BIT.keyBytes > size) {
            return null;
        }

        // Add keys up to the limit
        int bytesToGo = size - message.getSize();
        while (bytesToGo >= EIEIOType.KEY_32_BIT.keyBytes && vertex.isNextKey(region, nextTimestamp)){
           message.addKey(vertex.getNextKey(region));
           bytesToGo -= EIEIOType.KEY_32_BIT.keyBytes;
        }

        return message;
    }

    private void getDataForVerticesLocked(List<Vertex> vertices, ProgressBar progress) throws IOException, Process.Exception {
        LinkedHashSet<DataSpeedUpPacketGatherMachineVertex> receivers = new LinkedHashSet<>();

        if (usesAdvancedMonitors) {

            // locate receivers
            for (Vertex vertex : vertices) {
                Placement placement = placements.getPlacementOfVertex(vertex);
                receivers.add(locateExtraMonitorMcReceiver(placement));
            }

            // set time out
            for (DataSpeedUpPacketGatherMachineVertex receiver:  receivers) {
                receiver.setCoresForDataExtraction(transceiver, placements, extraMonitorCores);
            }

        }

        // get data
        for (Vertex vertex: vertices) {
            Placement placement = placements.getPlacementOfVertex(vertex);
            for (int recordingRegionId : vertex.getRecordedRegionIds()) {
                getDataForVertex(placement, recordingRegionId);
                if (progress != null) {
                    progress.update();
                }
            }
        }

//        # revert time out
//        if self._uses_advanced_monitors:
//            for receiver in receivers:
//                receiver.unset_cores_for_data_extraction(
//                    transceiver=self._transceiver, placements=self._placements,
//                    extra_monitor_cores_for_router_timeout=(
//                        self._extra_monitor_cores))
    }

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/utilities/helpful_functions.py
    private DataSpeedUpPacketGatherMachineVertex locateExtraMonitorMcReceiver(HasChipLocation placement){
        Chip chip = machine.getChipAt(placement);
        ChipLocation ethernet = chip.nearestEthernet.asChipLocation();
        return extraMonitorCoresToEthernetConnectionMap.get(ethernet);
    }

    private void readSomeData(
            Placement placement, int recordingRegionId, int address, int length) 
            throws IOException, Process.Exception {
        log.debug("< Reading " + length + " bytes from "
            + placement.asCoreLocation() + ": " + address
            + " for region " + recordingRegionId);
        ByteBuffer data = requestData(placement, address, length);
        receivedData.flushingDataFromRegion(placement, recordingRegionId, data);
    }
    
    private BufferedDataStorage getDataForVertexLocked(Placement placement, int recordingRegionId) throws IOException, Process.Exception {

        Vertex vertex = placement.getVertex();
        int recordingDataAddress = vertex.getRecordingRegionBaseAddress(transceiver, placement);

        // TODO Just because we have A sequence number can we assume it is the last one?
        // Ensure the last sequence number sent has been retrieved
        if (!receivedData.isEndBufferingSequenceNumberStored(placement)) {
            receivedData.storeEndBufferingSequenceNumber(placement,
                getLastSequenceNumber(placement, recordingDataAddress));
        }

        // Read the data if not already received
        if (receivedData.isDataFromRegionFlushed(placement, recordingRegionId)) {

            // Read the end state of the recording for this region
            ChannelBufferState endState;
            if (!receivedData.isEndBufferingStateRecovered(placement, recordingRegionId)) {
                int regionPointer = this.getRegionPointer(placement, recordingDataAddress, recordingRegionId);
                endState = generateEndBufferingStateFromMachine(placement, regionPointer);
                receivedData.storeEndBufferingState(placement, recordingRegionId, endState);
            } else {
                endState = receivedData.getEndBufferingState(placement, recordingRegionId);
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
                processLastAck(placement, recordingRegionId, endState);
            }

            // now state is updated, read back values for read pointer and
            // last operation performed
            //last_operation = end_state.last_buffer_operation
            //start_ptr = end_state.start_address
            //end_ptr = end_state.end_address
            //write_ptr = end_state.current_write
            //read_ptr = end_state.current_read

            if (endState.currentRead < endState.currentWrite) {
                int length = endState.currentWrite - endState.currentRead;
                readSomeData(placement, recordingRegionId, endState.currentRead, length); 
            } else if (endState.currentRead > endState.currentWrite || 
                    endState.getLastBufferOperation() == BufferingOperation.BUFFER_WRITE) {
                int length = endState.endAddress - endState.currentRead;
                if (length < 0) {
                    throw new IOException ("The amount of data to read is negative!");
                }        
                readSomeData(placement, recordingRegionId, endState.currentRead, length); 
                length = endState.endAddress - endState.startAddress;
                readSomeData(placement, recordingRegionId, endState.startAddress, length); 
            } else {
               ByteBuffer data = ByteBuffer.allocate(0);
               receivedData.flushingDataFromRegion(placement, recordingRegionId, data);
            }
        }
        // data flush has been completed - return appropriate data
        // the two returns can be exchanged - one returns data and the other
        // returns a pointer to the structure holding the data
        return receivedData.getRegionDataPointer(placement, recordingRegionId);
    }

    //Found in SpiNNFrontEndCommon/spinn_front_end_common/interface/buffer_management/recording_utilities.py
    private int getLastSequenceNumber(Placement placement, int recordingDataAddress) throws IOException, Process.Exception {
        ByteBuffer data = transceiver.readMemory(placement, recordingDataAddress + LAST_SEQUENCE_NUMBER_OFFSET, 4);
        return data.getInt(0);
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
        ByteBuffer data = transceiver.readMemory(placement, recordingDataAddress + FIRST_REGION_ADDRESS_OFFSET, (region * 4));
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
    private ByteBuffer requestData(Placement placement, int address, int length) throws IOException, Process.Exception {
        //    :return: data as a byte array
        if (!this.usesAdvancedMonitors) {
            return transceiver.readMemory(placement, address, length);
        }

        ExtraMonitorSupportMachineVertex sender = extraMonitorCoresByChip.get(placement.asChipLocation());
        DataSpeedUpPacketGatherMachineVertex receiver = locateExtraMonitorMcReceiver(placement);
        Placement senderPlacement = placements.getPlacementOfVertex(sender);
        return receiver.getData(transceiver, senderPlacement, address, length, fixedRoutes);
    }

    private void processLastAck(Placement placement, int recordingRegionId, ChannelBufferState endState) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
/*      # if the last ACK packet has not been processed on the chip,
        # process it now
        last_sent_ack = self._received_data.last_sent_packet_to_core(
            placement.x, placement.y, placement.p)
        last_sent_ack = create_eieio_command.read_eieio_command_message(
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
*/
    }
}
