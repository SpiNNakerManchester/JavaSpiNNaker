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

    public void loadInitialBuffers() throws IOException, Process.Exception {
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
                tag.getIPAddress().toString());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress().toString());
        return connection;
    }

    //Type of UDPConnection and MessageHandler
    private UDPConnection<EIEIOMessage<? extends EIEIOHeader>> createConnection1(IPTag tag) throws IOException {
        MessageHandler<EIEIOMessage<? extends EIEIOHeader>> callback = receiveBufferCommandMessage();
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> connectionFactory = new EIEIOConnectionFactory();
        UDPConnection<EIEIOMessage<? extends EIEIOHeader>> connection = tranceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory(), tag.getPort(),
                tag.getIPAddress().toString());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress().toString());
        return connection;
    }

    private UDPConnection<EIEIOMessage> createConnection2(IPTag tag) throws IOException {
        MessageHandler<EIEIOMessage> callback = receiveBufferCommandMessage2();
        UDPTransceiver.ConnectionFactory<UDPConnection<EIEIOMessage<? extends EIEIOHeader>>> connectionFactory = new EIEIOConnectionFactory();
        UDPConnection<EIEIOMessage> connection = tranceiver.registerUDPListener(
                callback, new EIEIOConnectionFactory2(), tag.getPort(),
                tag.getIPAddress().toString());
        seenTags.get(tag.getIPAddress()).add(connection.getLocalPort());
        connection.sendPortTriggerMessage(tag.getBoardAddress().toString());
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

    private void sendInitialMessages(Vertex vertex, Integer region, ProgressBar progress) throws IOException, Process.Exception {
        Placement placement = placements.getPlacementOfVertex(vertex);
        int regionBaseAddress = locateMemoryRegionForPlacement(placement, region, tranceiver);
        // Add packets until out of space
        boolean sentMessage = false;
        int bytesToGo = vertex.getRegionBufferSize(region);
        if (bytesToGo % 2 != 0) {
            throw new Error(
                    "The buffer region of " + vertex + " must be divisible by 2");
        }
        //allData = = b""
        if (vertex.isEmpty(region))
            sentMessage = true;
        else {
            while (vertex.isNextTimestamp(region) &&
                    bytesToGo > MIN_MESSAGE_SIZE) {
                int spaceAvailable = Math.min(bytesToGo, 280);
                createMessageToSend(spaceAvailable, vertex, region);

                //next_message = self._create_message_to_send(
                //    space_available, vertex, region)
                //if next_message is None:
                //    break

                //# Write the message to the memory
                //data = next_message.bytestring
                //all_data += data
                //sent_message = True

                //# Update the positions
                //bytes_to_go -= len(data)
                //progress.update(len(data))
            }
        }
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

    private Object createMessageToSend(int spaceAvailable, Vertex vertex, Integer region) {
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

        //message = EIEIODataMessage.create(
        //    EIEIOType.KEY_32_BIT, timestamp=next_timestamp)

        //# If there is no room for the message, return None
        //if message.size + _N_BYTES_PER_KEY > size:
        //    return None

        //# Add keys up to the limit
        //bytes_to_go = size - message.size
        //while (bytes_to_go >= _N_BYTES_PER_KEY and
        //        vertex.is_next_key(region, next_timestamp)):

        //    key = vertex.get_next_key(region)
        //    message.add_key(key)
        //    bytes_to_go -= _N_BYTES_PER_KEY

        //return message


        //TODO
        return null;
    }
}
