/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
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

    /**
     * Main Constructor that sets all values.
     *
     * @param height
     *            The height of the Machine in Chips
     * @param width
     *            The width of the Machine in Chips
     * @param root
     *            The Root Chip. (Typically 0,0)
     * @param ethernetResources
     *            The resource values shared by all chips that have an
     *            ip_address, expect when overwritten by the Chip itself.
     * @param standardResources
     *            The resource values shared by all chips that do not have an
     *            ip_address, expect when overwritten by the Chip itself.
     * @param chips
     *            Beans for each Chips on the machine.
     */
    public MachineBean(
            @JsonProperty(value = "height", required = true) int height,
            @JsonProperty(value = "width", required = true) int width,
            @JsonProperty(value = "root", required = true) ChipLocation root,
            @JsonProperty(value = "ethernetResources", required = true)
            ChipResources ethernetResources,
            @JsonProperty(value = "standardResources", required = true)
            ChipResources standardResources,
            @JsonProperty(value = "chips", required = true)
            List<ChipBean> chips) {
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

    @Override
    public String toString() {
        return dimensions + " root: " + root + "# Chips: " + chips.size();
    }

    /**
     * Longer String representation over several lines.
     *
     * @return A description of the machine and its details.
     */
    public String describe() {
        StringBuilder builder = new StringBuilder();
        builder.append(dimensions);
        builder.append("\nroot: ").append(root);
        builder.append("\nethernet_resources: ").append(ethernetResources);
        builder.append("\nstandard_resources: ").append(standardResources);
        for (ChipBean bean : chips) {
            builder.append("\n" + bean);
        }
        return builder.toString();
    }

}
