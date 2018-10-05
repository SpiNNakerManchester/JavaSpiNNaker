/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import uk.ac.manchester.spinnaker.machine.HasChipLocation;

/**
 *
 * @author Christian-B
 */
public class ChipResources {

    public static final int NOT_SET = -1;
    private int cores;
    private int sdram;
    private int tags;
    private int router_entries;
    private int sram;

    public ChipResources() {
        cores = NOT_SET;
        sdram = NOT_SET;
        tags = NOT_SET;
        router_entries = NOT_SET;
        sram = NOT_SET;
    }

    /**
     * @return the cores
     */
    public int getCores() {
        return cores;
    }

    /**
     * @param cores the cores to set
     */
    public void setCores(int cores) {
        this.cores = cores;
    }

    /**
     * @return the sdram
     */
    public int getSdram() {
        return sdram;
    }

    /**
     * @param sdram the sdram to set
     */
    public void setSdram(int sdram) {
        this.sdram = sdram;
    }

    /**
     * @return the tags
     */
    public int getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(int tags) {
        this.tags = tags;
    }

    /**
     * @return the router_entries
     */
    public int getRouter_entries() {
        return router_entries;
    }

    /**
     * @param router_entries the router_entries to set
     */
    public void setRouter_entries(int router_entries) {
        this.router_entries = router_entries;
    }

    /**
     * @return the sram
     */
    public int getSram() {
        return sram;
    }

    /**
     * @param sram the sram to set
     */
    public void setSram(int sram) {
        this.sram = sram;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        if (cores != NOT_SET) {
            builder.append("cores: ").append(cores).append(", ");
        }
        if (sdram != NOT_SET) {
            builder.append("sdram: ").append(sdram).append(", ");
        }
        if (sdram != NOT_SET) {
            builder.append("tags: ").append(tags).append(", ");
        }
        if (sdram != NOT_SET) {
            builder.append("router_entries: ").append(router_entries)
                    .append(", ");
        }
        if (sdram != NOT_SET) {
            builder.append("sram: ").append(sram).append(", ");
        }
        builder.setLength(builder.length() - 2);
        builder.append("]");
        return builder.toString();
    }


}
