/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import uk.ac.manchester.spinnaker.connections.UDPConnection;
import uk.ac.manchester.spinnaker.connections.model.MessageHandler;
import uk.ac.manchester.spinnaker.machine.tags.IPTag;
import uk.ac.manchester.spinnaker.machine.tags.TrafficIdentifer;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIODataMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOHeader;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOMessage;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOPrefix;
import uk.ac.manchester.spinnaker.messages.eieio.EIEIOType;
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

    //EIEIODataMessage.min_packet_length(
    //eieio_type=EIEIOType.KEY_32_BIT, is_timestamp=True)
    private static final int MIN_MESSAGE_SIZE = 4;

    Placements placements;
    Tags tags;
    Transceiver tranceiver;
    DefaultMap<InetAddress, HashSet> seenTags = new DefaultMap<>(HashSet::new);
    HashSet<Vertex> senderVertices = new HashSet<>();
    BufferedReceivingData receivedData;
    boolean finished;
    Integer listenerPort;  //may be null


    public BufferManager(Placements placements, Tags tags, Transceiver tranceiver, boolean storeToFile) {
        this.placements = placements;
        this.tags = tags;
        this.tranceiver = tranceiver;
        BufferedReceivingData receivedData = new BufferedReceivingData(storeToFile);
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
        UDPConnection connection = tranceiver.registerUDPListener(
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
        UDPConnection<EIEIOMessage<? extends EIEIOHeader>> connection = tranceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory(), tag.getPort(),
                tag.getIPAddress());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress());
        return connection;
    }

    private UDPConnection<EIEIOMessage> createConnection2(IPTag tag) throws IOException {
        MessageHandler<EIEIOMessage> callback = receiveBufferCommandMessage2();
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> connectionFactory = new EIEIOConnectionFactory();
        UDPConnection<EIEIOMessage> connection = tranceiver.registerUDPListener(
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
        int regionBaseAddress = locateMemoryRegionForPlacement(placement, region, tranceiver);
        // Add packets until out of space
        boolean sentMessage = false;
        int bytesToGo = vertex.getRegionBufferSize(region);
        if (bytesToGo % 2 != 0) {
            throw new Error(
                    "The buffer region of " + vertex + " must be divisible by 2");
        }
        //TODO: verify if bytesToGo is big enough (Python has no capacity)
        ByteBuffer allData = ByteBuffer.allocate(bytesToGo);
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
        /*
        # If there are no more messages and there is space, add a stop request
        if (not vertex.is_next_timestamp(region) and
                bytes_to_go >= EventStopRequest.get_min_packet_length()):
            data = EventStopRequest().bytestring
            # logger.debug(
            #    "Writing stop message of {} bytes to {} on {}, {}, {}".format(
            #         len(data), hex(region_base_address),
            #         placement.x, placement.y, placement.p))
            all_data += data
            bytes_to_go -= len(data)
            progress.update(len(data))
            self._sent_messages[vertex] = BuffersSentDeque(
                region, sent_stop_message=True)

        # If there is any space left, add padding
        if bytes_to_go > 0:
            padding_packet = PaddingRequest()
            n_packets = bytes_to_go // padding_packet.get_min_packet_length()
            data = padding_packet.bytestring
            data *= n_packets
            all_data += data

        # Do the writing all at once for efficiency
        self._transceiver.write_memory(
            placement.x, placement.y, region_base_address, all_data)
            */
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
}
