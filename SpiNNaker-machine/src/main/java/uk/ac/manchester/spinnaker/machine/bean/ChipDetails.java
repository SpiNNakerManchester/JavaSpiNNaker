/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import static java.util.Collections.emptyList;
import java.util.List;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public class ChipDetails {

    public final int cores;
    public final ChipLocation ethernet;
    private InetAddress ipAddress;
    private List<Direction> deadDirections = emptyList();

    public ChipDetails(@JsonProperty(value = "cores", required=true) int cores,
            @JsonProperty(value = "ethernet", required=true) ChipLocation ethernet) {
        this.cores = cores;
        this.ethernet = ethernet;
    }

    /**
     * @return the number of cores.
     */
    public int getCores() {
        return cores;
    }

    /**
     * @return the ethernet
     */
    public ChipLocation getEthernet() {
        return ethernet;
    }

    /**
     * @return the ipAddress
     */
    public InetAddress getIpAddress() {
        return ipAddress;
    }

    /**
     * @param ipAddress the ipAddress to set
     */
    public void setIpAddress(String ipAddress) throws UnknownHostException {
        this.ipAddress = InetAddress.getByName(ipAddress);
    }

    /**
     * @return the deadLinks
     */
    @JsonIgnore
    public List<Direction> getDeadDirections() {
        return deadDirections;
    }

    /**
     * @param deadLinks the deadLinks to set
     */
    public void setDeadLinks(List<Integer> deadLinks) {
        deadDirections = new ArrayList<>();
        for (Integer deadLink: deadLinks) {
            deadDirections.add(Direction.byId(deadLink));
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        builder.append("ethernet: ").append(ethernet).append(", ");
        builder.append(" cores: ").append(cores).append(", ");
        if (ipAddress != null) {
            builder.append(" ipAddress: ").append(ipAddress).append(", ");
        }
        if (deadDirections != null) {
            builder.append(" deadLinks:").append(deadDirections).append(", ");
        }
        builder.setLength(builder.length() - 2);
        builder.append("]");
        return builder.toString();
    }



}
