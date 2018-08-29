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
import java.util.List;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
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
 * @author Christian-B
 */
public class BufferManager {

    //Found in DataSpecification/data_specification/constants.py
    private static final int APP_PTR_TABLE_HEADER_BYTE_SIZE = 8;

    //2 bytes of generic EIEIO header, 4 bytes of key (for 32 bits)
    //and 0 bytes of payload (as it is KEY_32_BIT, not KEY_PAYLOAD_32_BIT).
    private static final int MIN_MESSAGE_SIZE = 6;

    Placements placements;
    Tags tags;
    Transceiver transceiver;
    DefaultMap<InetAddress, HashSet> seenTags = new DefaultMap<>(HashSet::new);
    HashSet<Vertex> senderVertices = new HashSet<>();
    BufferedReceivingData receivedData;
    volatile boolean finished;
    Integer listenerPort;  //may be null
    HashMap<Vertex,BuffersSentDeque> sentMessages = new HashMap<>();
    final boolean usesAdvancedMonitors;


    public BufferManager(Placements placements, Tags tags, Transceiver tranceiver,
            boolean usesAdvancedMonitors, boolean storeToFile) {
        this.placements = placements;
        this.tags = tags;
        this.transceiver = tranceiver;
        this.usesAdvancedMonitors = usesAdvancedMonitors;

        receivedData = new BufferedReceivingData(storeToFile);
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

    public void getDataForVertices(List<Vertex> vertices, ProgressBar progress){
        // TODO  with self._thread_lock_buffer_out:
        getDataForVerticesLocked(vertices, progress);
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

    private void getDataForVerticesLocked(List<Vertex> vertices, ProgressBar progress) {
        LinkedHashSet<Vertex> = new LinkedHashSet<>();

        if (usesAdvancedMonitors) {

            // locate receivers
            for (Vertex vertex : vertices) {
                Placement placement = placements.getPlacementOfVertex(vertex);
                receivers.add(funs.locate_extra_monitor_mc_receiver(
                    self._machine, placement.x, placement.y,
                    self._extra_monitor_cores_to_ethernet_connection_map))
            }

            # set time out
            for receiver in receivers:
                receiver.set_cores_for_data_extraction(
                    transceiver=self._transceiver, placements=self._placements,
                    extra_monitor_cores_for_router_timeout=(
                        self._extra_monitor_cores))

        }
        # get data
        for vertex in vertices:
            placement = self._placements.get_placement_of_vertex(vertex)
            for recording_region_id in vertex.get_recorded_region_ids():
                self.get_data_for_vertex(placement, recording_region_id)
                if progress is not None:
                    progress.update()

        # revert time out
        if self._uses_advanced_monitors:
            for receiver in receivers:
                receiver.unset_cores_for_data_extraction(
                    transceiver=self._transceiver, placements=self._placements,
                    extra_monitor_cores_for_router_timeout=(
                        self._extra_monitor_cores))
    }
}
