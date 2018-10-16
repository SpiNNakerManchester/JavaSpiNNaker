/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * Bean to represent values on a Chip that are typically the same on all Chips.
 *
 * @author Christian-B
 */
public class ChipResources {

    /** Symbolic value to specify no specific value has been set.
     *
     *  This allows the value 0 to be declared as specifically set.
     */
    public static final int NOT_SET = -1;
    private int cores;
    private int monitors;
    private int sdram;
    private List<Integer> tags;
    private int routerEntries;
    private int routerClockSpeed;
    private Boolean virtual;

    /**
     * Default constructor which sets all values to not set.
     */
    public ChipResources() {
        cores = NOT_SET;
        monitors = NOT_SET;
        sdram = NOT_SET;
        routerClockSpeed = NOT_SET;
        routerEntries = NOT_SET;
    }

    /**
     * Adds the default values
     *    if and only if no value had been specifically set.
     *
     * If a value is not set in both this and the defaults
     *      it will remain as not set. No Exception is thrown.
     *
     * @param defaults Another resources whose values should replace those
     *    which have not been set.
     */
    @JsonIgnore
    public void addDefaults(ChipResources defaults) {
        if (cores == NOT_SET) {
            cores = defaults.cores;
        }
        if (monitors == NOT_SET) {
            monitors = defaults.monitors;
        }
        if (sdram == NOT_SET) {
            sdram = defaults.sdram;
        }
        if (tags == null) {
            tags = defaults.tags;
        }
        if (getRouterClockSpeed() == NOT_SET) {
            setRouterClockSpeed(defaults.getRouterClockSpeed());
        }
        if (routerEntries == NOT_SET) {
            routerEntries = defaults.routerEntries;
        }
        if (virtual == null) {
            virtual = defaults.getVirtual();
        }
    }

    /**
     * @return the number of cores.
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
     * @return the monitors
     */
    public int getMonitors() {
        return monitors;
    }

    /**
     * @param monitors the monitors to set
     */
    public void setMonitors(int monitors) {
        this.monitors = monitors;
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
    public List<Integer> getTags() {
        return tags;
    }

    /**
     * @param tags the tags to set
     */
    public void setTags(List<Integer> tags) {
        this.tags = tags;
    }

    /**
     * @return the routerClockSpeed
     */
    public int getRouterClockSpeed() {
        return routerClockSpeed;
    }

    /**
     * @param routerClockSpeed the routerClockSpeed to set
     */
    public void setRouterClockSpeed(int routerClockSpeed) {
        this.routerClockSpeed = routerClockSpeed;
    }

    /**
     * @return the router_entries
     */
    public int getRouterEntries() {
        return routerEntries;
    }

    /**
     * @param routerEntries the router_entries to set
     */
    public void setRouterEntries(int routerEntries) {
        this.routerEntries = routerEntries;
    }

    /**
     * @return the virtual
     */
    public Boolean getVirtual() {
        return virtual;
    }

    /**
     * @param virtual the virtual to set
     */
    public void setVirtual(Boolean virtual) {
        this.virtual = virtual;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[");
        if (cores != NOT_SET) {
            builder.append("cores: ").append(cores).append(", ");
        }
        if (monitors != NOT_SET) {
            builder.append("monitors: ").append(monitors).append(", ");
        }
        if (sdram != NOT_SET) {
            builder.append("sdram: ").append(sdram).append(", ");
        }
        if (tags != null) {
            builder.append("tags: ").append(tags).append(", ");
        }
        if (getRouterClockSpeed() != NOT_SET) {
            builder.append("routerClockSpeed: ").append(getRouterClockSpeed())
                    .append(", ");
        }
        if (routerEntries != NOT_SET) {
            builder.append("router_entries: ").append(routerEntries)
                    .append(", ");
        }
        if (builder.length() < 2) {
            builder.append("EMPTY");
        }
        builder.setLength(builder.length() - 2);
        builder.append("]");
        return builder.toString();
    }


}
