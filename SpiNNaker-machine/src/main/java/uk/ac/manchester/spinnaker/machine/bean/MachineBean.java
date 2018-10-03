/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import java.util.List;
import static java.util.Collections.emptyList;

/**
 *
 * @author Christian-B
 */
public class MachineBean {

    private int height;
    private int width;
    private List<ChipLocationBean> deadChips  = emptyList();
    private List<ChipResourceException> chip_resource_exceptions  = emptyList();
    private ChipResources chip_resources = new ChipResources();

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * @return the deadChips
     */
    public List<ChipLocationBean> getDeadChips() {
        return deadChips;
    }

    /**
     * @param deadChips the deadChips to set
     */
    public void setDeadChips(List<ChipLocationBean> deadChips) {
        this.deadChips = deadChips;
    }

    /**
     * @return the chip_resources
     */
    public ChipResources getChip_resources() {
        return chip_resources;
    }

    /**
     * @param chip_resources the chip_resources to set
     */
    public void setChip_resources(ChipResources chip_resources) {
        this.chip_resources = chip_resources;
    }

    /**
     * @return the chip_resource_exceptions
     */
    public List<ChipResourceException> getChip_resource_exceptions() {
        return chip_resource_exceptions;
    }

    /**
     * @param chip_resource_exceptions the chip_resource_exceptions to set
     */
    public void setChip_resource_exceptions(List<ChipResourceException> chip_resource_exceptions) {
        this.chip_resource_exceptions = chip_resource_exceptions;
    }

    public String toString() {
        return "width: " + width + " height " + height
                + "\nchip_resources: " + chip_resources
                + "\nchip_resource_exceptions: " + chip_resource_exceptions
                + "\ndead_chips: " + deadChips ;
    }

}
