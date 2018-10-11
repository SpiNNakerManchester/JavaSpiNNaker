/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import static java.util.Collections.emptyMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import uk.ac.manchester.spinnaker.machine.Chip;
import uk.ac.manchester.spinnaker.machine.ChipLocation;
import uk.ac.manchester.spinnaker.machine.MachineDimensions;

/**
 *
 * @author Christian-B
 */
public class MachineBean {

    private final MachineDimensions dimensions;
    private final ChipLocation root;
    private final ChipResources ethernetResources;
    private final ChipResources standardResources;
    private final List<ChipBean> chips;

    public MachineBean(@JsonProperty(value = "height", required=true) int height,
            @JsonProperty(value = "width", required=true) int width,
            @JsonProperty(value = "root", required=true) ChipLocation root,
            @JsonProperty(value = "ethernetResources", required=true) ChipResources ethernetResources,
            @JsonProperty(value = "standardResources", required=true) ChipResources standardResources,
            @JsonProperty(value = "chips", required=true) List<ChipBean> chips) {
        dimensions = new MachineDimensions(height, width);
        this.root = root;
        this.chips = chips;
        this.ethernetResources = ethernetResources;
        this.standardResources = standardResources;
    }

    @JsonIgnore
    public MachineDimensions getMachineDimensions() {
        return dimensions;
    }

    public ChipLocation getRoot() {
        return root;
    }

    /**
     * @return the default ethernet resources
     */
    public ChipResources getEthernetResources() {
        return ethernetResources;
    }

    /**
     * @return the default none ethernet resources
     */
    public ChipResources getStandardResources() {
        return standardResources;
    }

   /**
     * @return the chips
     */
    public List<ChipBean> getChips() {
        return chips;
    }

    public String toString() {
        return dimensions + " root: " + root
                + "# Chips: " + chips.size();
    }

    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append(dimensions);
        builder.append("\nroot: ").append(root);
        builder.append("\nethernet_resources: ").append(ethernetResources);
        builder.append("\nstandard_resources: ").append(standardResources);
        for (ChipBean bean: chips) {
            builder.append("\n" + bean);
        }
        return builder.toString();
    }

 }
