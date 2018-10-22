/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.front_end.interfaces.buffer_management;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import uk.ac.manchester.spinnaker.machine.CoreLocation;
import uk.ac.manchester.spinnaker.transceiver.Transceiver;
import uk.ac.manchester.spinnaker.transceiver.processes.Process;

/**
 *
 * @author Christian-B
 */
class DataSpeedUpPacketGatherMachineVertex {

    public static final int THRESHOLD_WHERE_SDP_BETTER_THAN_DATA_EXTRACTOR_IN_BYTES = 40000;

/*    void setCoresForDataExtraction(Transceiver transceiver, Placements placements, List<ExtraMonitorSupportMachineVertex> extraMonitorCores) {
        //# Store the last reinjection status for resetting
        //# NOTE: This assumes the status is the same on all cores

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        Object lastStatus = extraMonitorCores.get(0).getReinjectionStatus(
                placements, transceiver)

        # Set to not inject dropped packets
        extra_monitor_cores_for_router_timeout[0].set_reinjection_packets(
            placements, extra_monitor_cores_for_router_timeout, transceiver,
            point_to_point=False, multicast=False, nearest_neighbour=False,
            fixed_route=False)

        # Clear any outstanding packets from reinjection
        extra_monitor_cores_for_router_timeout[0].clear_reinjection_queue(
            transceiver, placements, extra_monitor_cores_for_router_timeout)

        # set time out
        extra_monitor_cores_for_router_timeout[0].set_router_time_outs(
            15, 15, transceiver, placements,
            extra_monitor_cores_for_router_timeout)
        extra_monitor_cores_for_router_timeout[0].\
            set_reinjection_router_emergency_timeout(
                1, 1, transceiver, placements,
                extra_monitor_cores_for_router_timeout)
    }
*/
    ByteBuffer getData(Transceiver transceiver, Placement senderPlacement,
            int address, int lengthInBytes, Map<CoreLocation,
                    FixedRouteEntry> fixedRoutes) throws IOException, Process.Exception {
        // read thingie needs to be beginning of the data
        //ByteBuffer is little Endian
        // TODO
        //start = float(time.time())

        //# if asked for no data, just return a empty byte array
        if (lengthInBytes == 0) {
            // TODO
            //end = float(time.time())
            //self._provenance_data_items[
            //    placement, memory_address,
            //    length_in_bytes].append((end - start, [0]))
            return ByteBuffer.allocate(0);
        }
        //if (lengthInBytes <
        //        THRESHOLD_WHERE_SDP_BETTER_THAN_DATA_EXTRACTOR_IN_BYTES){
            // TODO
            //end = float(time.time())
            //self._provenance_data_items[
            //    placement, memory_address,
            //    length_in_bytes].append((end - start, [0]))
            return transceiver.readMemory(senderPlacement, address, lengthInBytes);
        //}
/*        data = _THREE_WORDS.pack(
            self.SDP_PACKET_START_SENDING_COMMAND_ID,
            memory_address, length_in_bytes)

        # logger.debug("sending to core %d:%d:%d",
        #              placement.x, placement.y, placement.p)

        # send
        self._connection.send_sdp_message(SDPMessage(
            sdp_header=SDPHeader(
                destination_chip_x=placement.x,
                destination_chip_y=placement.y,
                destination_cpu=placement.p,
                destination_port=self.SDP_PORT,
                flags=SDPFlag.REPLY_NOT_EXPECTED),
            data=data))

        # receive
        self._output = bytearray(length_in_bytes)
        self._view = memoryview(self._output)
        self._max_seq_num = self.calculate_max_seq_num()
        lost_seq_nums = self._receive_data(transceiver, placement)

        end = float(time.time())
        self._provenance_data_items[
            placement, memory_address, length_in_bytes].append(
                (end - start, lost_seq_nums))

        # create report elements
        if self._write_data_speed_up_report:
            routers_been_in_use = self._determine_which_routers_were_used(
                placement, fixed_routes, transceiver.get_machine_details())
            self._write_routers_used_into_report(
                self._report_path, routers_been_in_use, placement)

        return self._output
*/
    }

/*    void unset_cores_for_data_extraction(Transceiver transceiver, Placements placements, List<ExtraMonitorSupportMachineVertex> extraMonitorCores) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
*/
}
