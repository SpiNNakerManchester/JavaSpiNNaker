/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.Direction;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;
import uk.ac.manchester.spinnaker.utils.DefaultMap;

/**
 *
 * @author Christian-B
 */
public class MachineBean {

    private final MachineDimensions dimensions;
    private List<ChipLocation> deadChips = emptyList();
    private Map<ChipLocation, ChipResources> chipResourceExceptions  = emptyMap();
    private ChipResources chipResources = null;
    private Map<ChipLocation, Collection<Direction>> ignoreLinks =
            new DefaultMap<>(HashSet<Direction>::new);
    private Map<ChipLocation, InetAddress>  ipAddresses = emptyMap();

    public MachineBean(@JsonProperty(value = "height", required=true) int height,
            @JsonProperty(value = "width", required=true) int width) {
        dimensions = new MachineDimensions(height, width);
    }

    @JsonIgnore
    public MachineDimensions getMachineDimensions() {
        return dimensions;
    }

    /**
     * @return the deadChips
     */
    public List<ChipLocation > getDeadChips() {
        return deadChips;
    }

    /**
     * @param deadChips the deadChips to set
     */
    public void setDeadChips(List<ChipLocation> deadChips) {
        this.deadChips = deadChips;
    }

    /**
     * @return the chip_resources
     */
    public ChipResources getChipResources() {
        return chipResources;
    }

    /**
     * @param chip_resources the chip_resources to set
     */
    public void setChipResources(ChipResources chip_resources) {
        this.chipResources = chip_resources;
    }

    /**
     * @return the chip_resource_exceptions
     */
    @JsonIgnore
    public Map<ChipLocation, ChipResources>  getChipResourceExceptionsMap() {
        return chipResourceExceptions;
    }

    /**
     * Obtain the ChipRescoure exceptions for a single chip
     * @param location The x, y coordinates of a chip
     * @return The resources for this chip either because they have been
     *      specified or because they come from the general ones.
     */
    @JsonIgnore
    public ChipResources getChipResources(ChipLocation location) {
        ChipResources specific = chipResourceExceptions.get(location);
        if (specific == null) {
            return chipResources;
        }
        specific.merge(chipResources);
        return specific;
    }

    /**
     * @param chipResourceExceptions the chip_resource_exceptions to set
     */
    public void setChipResourceExceptions(
            List<ChipResourceException> chipResourceExceptions) {
        this.chipResourceExceptions =
                new HashMap<ChipLocation, ChipResources>();
        chipResourceExceptions.stream().forEach(
                bean -> this.chipResourceExceptions.put(
                        bean.location, bean.exceptions));
    }

    /**
     * @return the deadLinks
     */
    @JsonIgnore
    public Map<ChipLocation, Collection<Direction>> getIgnoreLinks() {
        return ignoreLinks;
    }

    /**
     * Obtain the deadlinks if any for this location
     * @param location The x, y coordinates of a chip
     * @return A collection of directions to ignore which may be empty.
     */
    public Collection<Direction> getIgnoreLinks(ChipLocation location) {
        return ignoreLinks.get(location);
    }

    /**
     * @param deadLinks the deadLinks to set
     */
    public void setDeadLinks(List<DeadLink> deadLinks) {
        ignoreLinks.clear();
        deadLinks.stream().forEach(
                bean -> this.ignoreLinks.get(bean.getLocation()).
                        add(bean.getDirection()));
    }

   /**
     * @return the ipAddresses
     */
    @JsonIgnore
    public Map<ChipLocation, InetAddress> getIpAddressMap() {
        return ipAddresses;
    }

    /**
     * Obtain the Ipaddress for this chip if any.
     *
     * @param location The x, y coordinates of a chip
     * @return The ip_address if it is known or null if not.
     */
    @JsonIgnore
    public InetAddress getIpAddress(ChipLocation location) {
        return ipAddresses.get(location);
    }

    /**
     * @param ipAddresses the ipAddresses to set
     */
    public void setIpAddresses(List<ChipIPAddress> ipAddresses) {
        this.ipAddresses =
                new HashMap<ChipLocation, InetAddress>();
        ipAddresses.stream().forEach(
                bean -> this.ipAddresses.put(
                        bean.location, bean.ipAddress));
    }

    public String toString() {
        return dimensions
                + "\nchip_resources: " + chipResources
                + "\nchip_resource_exceptions: " + chipResourceExceptions
                + "\ndead_chips: " + deadChips
                + "\nipAddresses:" + ipAddresses
                + "\nignoreLinks:" + ignoreLinks;
    }

 }
