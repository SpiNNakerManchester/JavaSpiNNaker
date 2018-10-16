/*
 * Copyright (c) 2018 The University of Manchester
 */
package uk.ac.manchester.spinnaker.machine.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import static com.fasterxml.jackson.annotation.JsonFormat.Shape.ARRAY;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.manchester.spinnaker.machine.ChipLocation;

/**
 *
 * @author Christian-B
 */
@JsonFormat(shape = ARRAY)
public class ChipBean {
    /** The location of this Chip. */
	public final ChipLocation location;
    /** The details for this Chip. */
    public final ChipDetails details;
    private ChipResources resources;

    /**
     * Main constructor with all values as properties.
     *
     * @param x X Coordinate of the Chip
     * @param y Y Coordinates of the Chip.
     * @param details c
     * @param resources Any resources specifically declared for this Chip.
     *      May be null.
     */
    @JsonCreator
    public ChipBean(
            @JsonProperty(value = "x", required = true) int x,
            @JsonProperty(value = "y", required = true) int y,
            @JsonProperty(value = "details", required = true)
                    ChipDetails details,
            @JsonProperty(value = "resources", required = false)
                    ChipResources resources) {
        location = new ChipLocation(x, y);
        this.details = details;
        this.resources = resources;
    }

    public ChipLocation getLocation() {
        return location;
    }

    /**
     * @return the resources
     */
    public ChipResources  getResources() {
        return resources;
    }

    /**
     * @return the resources
     */
    public ChipDetails  getDetails() {
        return details;
    }

    @Override
    public String toString() {
        if (resources != null) {
            return location + ", " + details + " " + resources;
        } else {
            return location + ", " + details + " DEFAULTS";
        }
    }

    /**
     * Adds the suitable default ChipResources.
     *
     * Based on if the Chip is an Ethernet one or not this will add the
     *      suitable resources.
     * Any values specifically set for a Chip have preference
     *      over the default values.
     * @param bean Main bean to copy defaults from.
     */
    public void addDefaults(MachineBean bean) {
        ChipResources defaults;
        if (details.getIpAddress() == null) {
            defaults = bean.getStandardResources();
        } else {
            defaults = bean.getEthernetResources();
        }
        if (resources == null) {
            resources = defaults;
        }
        resources.addDefaults(defaults);
    }
}
