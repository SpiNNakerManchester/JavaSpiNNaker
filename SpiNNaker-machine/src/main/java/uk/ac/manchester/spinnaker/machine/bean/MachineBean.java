/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    private List<ChipLocation> deadChips  = emptyList();
    private Map<ChipLocation, ChipResources> chipResourceExceptions  = emptyMap();
    private ChipResources chipResources = null;
    private Map<ChipLocation, Collection<Direction>> ignoreLinks = emptyMap();

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
        return null;
        //return chipResourceExceptions;
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
     * @param deadLinks the deadLinks to set
     */
    public void setDeadLinks(List<DeadLink> deadLinks) {
        ignoreLinks = new DefaultMap<>(HashSet<Direction>::new);
        deadLinks.stream().forEach(
                bean -> this.ignoreLinks.get(bean.getLocation()).
                        add(bean.getDirection()));
    }

    public String toString() {
        return dimensions
                + "\nchip_resources: " + chipResources
                + "\nchip_resource_exceptions: " + chipResourceExceptions
                + "\ndead_chips: " + deadChips
                + "\nignoreLinks:" + ignoreLinks;
    }
}
